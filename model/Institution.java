package model;

import java.time.LocalDate;

public class Institution {
    private int institutionId;
    private String name;
    private String type;
    private String address;
    private LocalDate registeredOn;
    private String status;
    private ContactPerson contact;


    public Institution(String name, String type, String address, ContactPerson contact){
        this.name = name;
        this.type = type;
        this.address = address;
        this.contact = contact;
    }
     

    public Institution(int institutionId, String name, String type, String address, LocalDate registeredOn, String status){
        this.institutionId = institutionId;
        this.name = name;
        this.type = type;
        this.address = address;
        this.registeredOn = registeredOn;
        this.status = status;
    }

    public int getInstitutionId() {
        return institutionId;
    }
    public String getName(){
        return name;
    }
    public String getType() {
        return type;
    }
    public String getAddress(){
        return address;
    }
    public LocalDate getRegisteredOn() {
        return registeredOn;
    }
    public String getStatus() {
        return status;
    }
    public ContactPerson getContact() {
        return contact;
    }

    public void setInstitutionId(int institutionId) {
        this.institutionId = institutionId;
    }
    public void setRegisteredOn( LocalDate registeredOn) {
        this.registeredOn = registeredOn;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public void setContact(ContactPerson contact) {
        this.contact = contact;
    }
}
