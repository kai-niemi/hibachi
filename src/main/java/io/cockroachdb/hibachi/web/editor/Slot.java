package io.cockroachdb.hibachi.web.editor;

import java.util.Objects;

public class Slot {
    public static final Slot ONE = new Slot("One");

    public static final Slot TWO = new Slot("Two");

    public static final Slot THREE = new Slot("Three");

    public static final Slot FOUR = new Slot("Four");

    private String name;

    private boolean occupied;

    public Slot(String name) {
        this.name = name;
    }

    public String getClassName() {
        return "btn btn-outline-" + (isOccupied() ?  "warning" : "success");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Slot slot = (Slot) o;
        return Objects.equals(name, slot.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return "Slot{" +
               "name='" + name + '\'' +
               ", occupied=" + occupied +
               '}';
    }
}
