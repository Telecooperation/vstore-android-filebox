package vstore.android_filebox.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import java.io.File;

/**
 * A helper class providing functions for working with Contacts and .vcf-files.
 */
public class ContactParser {

    public String id = null;
    public String name = null;
    public String phone = null;
    public String email = null;
    public String organization = null;
    public Address addr = null;
    public int hasPhone;

    public ContactParser() {}

    /**
     * This method gets the data for the given contact from the content provider.
     * By default, the name of the contact will be used as file name.
     * @param c The Android context.
     * @param contactUri The content uri to the contact.
     * @param outputPath Where to save the contact file
     * @param defaultName The default name of the contact file (if the contact has no name).
     */
    public File getAndWriteContactData(Context c, Uri contactUri, File outputPath, String defaultName) {
        String outputName = defaultName;
        ContentResolver resolver = c.getContentResolver();
        Cursor cur2;
        Cursor cur1 = resolver.query(contactUri, null, null, null, null);
        if (cur1 != null && cur1.moveToFirst()) {
            //Get Contact id
            try {
                id = cur1.getString(cur1.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
            } catch (IllegalArgumentException e) {
                cur1.close();
                return null;
            }
            //Get name
            try {
                name = cur1.getString(cur1.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
            } catch (IllegalArgumentException e) {
                name = "";
            }
            //Get phone number
            try {
                hasPhone = cur1.getInt(cur1.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                //Check if contact has at least one phone number
                if(hasPhone == 1) {
                    cur2 = resolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,
                            null, null);
                    if (cur2 != null && cur2.moveToFirst()) {
                        phone = cur2.getString(cur2.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        cur2.close();
                    }
                }
            } catch (IllegalArgumentException e) { }
            // Get email address
            cur2 = resolver.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = " + id,
                    null, null);
            if (cur2 != null && cur2.moveToFirst()) {
                email = cur2.getString(cur2.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
                cur2.close();
            }
            //Get organization
            cur2 = resolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    null,
                    ContactsContract.Data.CONTACT_ID + " = ? " +
                            "AND " + ContactsContract.Data.MIMETYPE + " = ?",
                    new String[] {
                            id,
                            ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE
                    },
                    null
            );
            if (cur2 != null && cur2.moveToFirst()) {
                String orgName = cur2.getString(cur2.getColumnIndex(ContactsContract.CommonDataKinds.Organization.DATA));
                String title = cur2.getString(cur2.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE));
                organization = title + ", " + orgName;
                cur2.close();
            }
            //Get home address
            cur2 = resolver.query(
                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                    null,
                    ContactsContract.Data.CONTACT_ID + "= ?",
                    new String[] { id },
                    null
            );
            if(cur2 != null && cur2.moveToFirst()) {
                addr = new Address();
                addr.street = cur2.getString(cur2.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.STREET));
                if(addr.street == null) addr.street = "";
                addr.city = cur2.getString(cur2.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.CITY));
                if(addr.city == null) addr.city = "";
                addr.country = cur2.getString(cur2.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY));
                if(addr.country == null) addr.country = "";
                cur2.close();
            }
            //Create a temporary vcf-file and return it so that the framework can copy it
            String vcf = createVcfString(name, "", organization, phone, null, addr, email);
            //Write the vcf-file to the given directory
            if(!name.equals("")) { outputName = name + ".vcf"; }
            return FileUtils.writeStringToFile(vcf, outputPath, outputName);
        }
        return null;
    }

    /**
     * Creates a vCard 3.0 String using the given information.
     * Any parameter you do not want to set can be set to null.
     * @param firstname The person's firstname.
     * @param lastname The person's lastname.
     * @param organization The organization where the person works.
     * @param phoneHome The first phone number of the person.
     * @param phoneWork A second phone number of the person.
     * @param addressHome The home address of the person as a {@link ContactParser.Address} object.
     * @param email The person's email address.
     * @return The vCard 3.0 String that can be written to a .vcf-file
     */
    public String createVcfString(String firstname, String lastname, String organization,
                                  String phoneHome, String phoneWork, Address addressHome,
                                  String email) {
        String vcf = "BEGIN:VCARD\n" +
                "VERSION:3.0\n" +
                "N:"+lastname+";"+firstname+";;\n" +
                "FN:"+firstname+" "+lastname+"\n" +
                ((organization != null) ? "ORG:"+organization+"\n" : "" )+
                ((phoneHome != null) ? ("TEL;TYPE=HOME,VOICE:"+phoneHome+"\n") : "" )+
                ((phoneWork != null) ? ("TEL;TYPE=WORK,VOICE:"+phoneWork+"\n") : "" )+
                ((addressHome != null) ?
                        ("ADR;TYPE=HOME:;;"+addressHome.street+";"+addressHome.city+";"+addressHome.country+"\n" +
                                "LABEL;TYPE=HOME:"+addressHome.street+"\\n"+addressHome.city+"\\n"+addressHome.country+"\n")
                        : "") +
                ((email != null) ? "EMAIL:"+email+"\n" : "" ) +
                "END:VCARD";
        return vcf;
    }

    public static class Address {
        public String street; //Street with house number
        public String city; //City with postcode
        public String country;
    }
}
