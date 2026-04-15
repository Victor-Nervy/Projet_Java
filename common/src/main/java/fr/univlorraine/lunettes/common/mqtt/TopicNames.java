package fr.univlorraine.lunettes.common.mqtt;

public final class TopicNames {

    private TopicNames() {
    }

    public static String order(String orderId) {
        return "orders/" + orderId;
    }

    public static String orderValidated(String orderId) {
        return order(orderId) + "/validated";
    }

    public static String orderCancelled(String orderId) {
        return order(orderId) + "/cancelled";
    }

    public static String orderDelivery(String orderId) {
        return order(orderId) + "/delivery";
    }

    public static String orderError(String orderId) {
        return order(orderId) + "/error";
    }

    public static String orderStatus(String orderId) {
        return order(orderId) + "/status";
    }

    public static String orderSubscription(String orderId) {
        return order(orderId) + "/#";
    }

    public static String ordersWildcard() {
        return "orders/+";
    }

    public static String serialCheck(String serial) {
        return "serials/" + serial + "/check";
    }

    public static String serialResult(String serial) {
        return "serials/" + serial;
    }

    public static String serialCheckWildcard() {
        return "serials/+/check";
    }
}
