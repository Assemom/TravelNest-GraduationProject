package com.travel.management.exception;

public class PackageNotFoundException extends ResourceNotFoundException {
    public PackageNotFoundException(Long id) {
        super("Package not found with id: " + id);
    }
}
