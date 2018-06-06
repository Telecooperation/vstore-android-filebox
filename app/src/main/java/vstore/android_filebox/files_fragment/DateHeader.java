package vstore.android_filebox.files_fragment;


import java.util.Date;

public class DateHeader {
    private String mString;
    private Date mDate;

    public DateHeader(Date date, String title) {
        mString = title;
        mDate = date;
    }

    public String getTitle() {
        return mString;
    }

    public Date getDate() {
        return mDate;
    }
}
