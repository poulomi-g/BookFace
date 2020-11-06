package com.example.bookface;

import android.location.Location;

import java.util.List;

/**
 * This is a class that contains the attributes and methods for sending a notification
 */
public class Notification {
    private Book requestedBook;
    private User borrower;
    private String requestStatus;
    private Location location;  // Not sure

    public Notification(Book bookRequested, User borrower, String requestStatus, Location location) {
        this.requestedBook = bookRequested;
        this.borrower = borrower;
        this.requestStatus = requestStatus;
        this.location = location;
    };

    public Book getRequestedBook() {
        return requestedBook;
    }

    public void setRequestedBook(Book requestedBook) {
        this.requestedBook = requestedBook;
    }

    public User getBorrower() {
        return borrower;
    }

    public void setBorrower(User borrower) {
        this.borrower = borrower;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }


    /**
     * This method displays the notification on the user screen
     * @return
     * True if it was success and false if it is wrong
     */
    public boolean displayNotification() {
        // TODO
        // Logic for this method

        return true;
    }
}
