package zw.co.cresolzim.schoolms.service;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String what, Object id) {
        super(what + " " + id + " does not exist");
    }
}
