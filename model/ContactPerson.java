package model;

public class ContactPerson{
    //private fields, belong to this class only

    private int contactId;
    private int institutionId;
    private String fullName;
    private String phone;
    private String email;

    //Constructor 1: a new contact typed into the form
    public ContactPerson(String fullName, String phone, String email){
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
    } 

    //Constructor 2
    public ContactPerson(int contactId, int institutionId, String fullName, String phone, String email){
        this.contactId = contactId;
        this.institutionId = institutionId;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
    }

    public int getContactId()          {  return contactId; }
    public int getInstitutionId()      {  return institutionId; }
    public String getFullName()        {  return fullName; }
    public String getPhone()           {  return phone; }
    public String getEmail()            {  return email; }



    public void setInstitutionId(int institutionId) {this.institutionId = institutionId; }
    public void setContactId(int contactId) {this.contactId = contactId; }
}