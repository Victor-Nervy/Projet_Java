import socket
import struct
import threading


HOST = "127.0.0.1"
PORT = 1883
subscribers = []
lock = threading.Lock()


def read_exact(sock, size):
    data = bytearray()
    while len(data) < size:
        chunk = sock.recv(size - len(data))
        if not chunk:
            raise ConnectionError("client closed")
        data.extend(chunk)
    return bytes(data)


def read_remaining_length(sock):
    multiplier = 1
    value = 0
    while True:
        encoded = read_exact(sock, 1)[0]
        value += (encoded & 127) * multiplier
        if encoded & 128 == 0:
            return value
        multiplier *= 128


def encode_remaining_length(value):
    encoded = bytearray()
    while True:
        digit = value % 128
        value //= 128
        if value > 0:
            digit |= 128
        encoded.append(digit)
        if value == 0:
            return bytes(encoded)


def encode_utf8(value):
    raw = value.encode("utf-8")
    return struct.pack("!H", len(raw)) + raw


def decode_utf8(payload, offset):
    size = struct.unpack("!H", payload[offset:offset + 2])[0]
    offset += 2
    return payload[offset:offset + size].decode("utf-8"), offset + size


def topic_matches(filter_name, topic):
    filter_parts = filter_name.split("/")
    topic_parts = topic.split("/")
    for index, part in enumerate(filter_parts):
        if part == "#":
            return True
        if index >= len(topic_parts):
            return False
        if part != "+" and part != topic_parts[index]:
            return False
    return len(filter_parts) == len(topic_parts)


def publish(topic, payload):
    packet_body = encode_utf8(topic) + payload
    packet = bytes([0x30]) + encode_remaining_length(len(packet_body)) + packet_body
    with lock:
        targets = [client for client, filters in subscribers if any(topic_matches(item, topic) for item in filters)]
    for client in targets:
        try:
            client.sendall(packet)
        except OSError:
            pass


def remove_client(sock):
    with lock:
        subscribers[:] = [(client, filters) for client, filters in subscribers if client is not sock]


def handle_client(sock, address):
    filters = []
    try:
        while True:
            fixed_header = read_exact(sock, 1)[0]
            packet_type = fixed_header >> 4
            flags = fixed_header & 0x0F
            remaining_length = read_remaining_length(sock)
            payload = read_exact(sock, remaining_length)

            if packet_type == 1:
                sock.sendall(b"\x20\x02\x00\x00")
            elif packet_type == 3:
                topic, offset = decode_utf8(payload, 0)
                qos = (flags >> 1) & 0x03
                packet_id = None
                if qos:
                    packet_id = payload[offset:offset + 2]
                    offset += 2
                publish(topic, payload[offset:])
                if qos == 1 and packet_id:
                    sock.sendall(b"\x40\x02" + packet_id)
            elif packet_type == 8:
                packet_id = payload[0:2]
                offset = 2
                granted = bytearray()
                while offset < len(payload):
                    topic_filter, offset = decode_utf8(payload, offset)
                    requested_qos = payload[offset]
                    offset += 1
                    filters.append(topic_filter)
                    granted.append(min(requested_qos, 1))
                with lock:
                    subscribers[:] = [(client, existing) for client, existing in subscribers if client is not sock]
                    subscribers.append((sock, filters))
                body = packet_id + bytes(granted)
                sock.sendall(bytes([0x90]) + encode_remaining_length(len(body)) + body)
            elif packet_type == 12:
                sock.sendall(b"\xD0\x00")
            elif packet_type == 14:
                break
    except (ConnectionError, OSError):
        pass
    finally:
        remove_client(sock)
        sock.close()


def main():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server:
        server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        server.bind((HOST, PORT))
        server.listen()
        print(f"Broker MQTT local ecoute sur {HOST}:{PORT}", flush=True)
        while True:
            client, address = server.accept()
            threading.Thread(target=handle_client, args=(client, address), daemon=True).start()


if __name__ == "__main__":
    main()
