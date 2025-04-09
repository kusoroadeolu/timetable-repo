package com.uni.TimeTable.exception;

// Custom exception for conflicts
public class ConflictException extends Exception {
    public ConflictException(String message) {
        super(message);
    }
}
