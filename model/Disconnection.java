package model;

import java.time.LocalDate;

// One cut-off of service, open until the institution clears its debt and reconnects.
public class Disconnection {

    private int disconnectionId;
    private final int institutionId;
    private final LocalDate disconnectedOn;
    private boolean reconnected;
    private LocalDate reconnectedOn;          // null while the disconnection stays open

    // Used when reading a saved row.
    public Disconnection(int disconnectionId, int institutionId, LocalDate disconnectedOn,
                         boolean reconnected, LocalDate reconnectedOn) {
        this.disconnectionId = disconnectionId;
        this.institutionId = institutionId;
        this.disconnectedOn = disconnectedOn;
        this.reconnected = reconnected;
        this.reconnectedOn = reconnectedOn;
    }

    // Used for a new cut-off; every new one starts open.
    public Disconnection(int institutionId, LocalDate disconnectedOn) {
        this(0, institutionId, disconnectedOn, false, null);
    }

    public boolean isOpen() { return !reconnected; }

    // Guard clauses protect the object's state. The service checks the money rules first;
    // these two checks catch a programming slip, so they throw unchecked exceptions.
    public void reconnect(LocalDate on) {
        if (reconnected) {
            throw new IllegalStateException("This disconnection is already closed.");
        }
        if (on.isBefore(disconnectedOn)) {
            throw new IllegalArgumentException("Reconnection cannot come before " + disconnectedOn + ".");
        }
        this.reconnected = true;
        this.reconnectedOn = on;
    }

    public int getDisconnectionId()     {
        return disconnectionId;
    }
    public int getInstitutionId() {
        return institutionId;
    }
    public LocalDate getDisconnectedOn() {
        return disconnectedOn;
    }
    public boolean isReconnected()      {
        return reconnected;
    }
    public LocalDate getReconnectedOn() {
        return reconnectedOn;
    }

    public void setDisconnectionId(int id) {
        this.disconnectionId = id;
    }
}