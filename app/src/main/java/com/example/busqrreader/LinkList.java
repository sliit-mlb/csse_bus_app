package com.example.busqrreader;

public class LinkList {
    private Link first;

    public LinkList() {
        first = null;
    }

    public boolean isEmpty() {
        return (first == null);
    }

    public void insertFirst(Link link) {
        link.next = first;
        first = link;
    }

    public Link deleteLink(String id) {
        Link current = first;
        Link previous = first;

        if (current == null)
            return null;

        while (!current.id.equals(id)) {
            previous = current;
            current = current.next;
            if (current == null)
                return null;
        }

        if (current == first)
            first = first.next;
        else
            previous.next = current.next;

        return current;
    }
}
