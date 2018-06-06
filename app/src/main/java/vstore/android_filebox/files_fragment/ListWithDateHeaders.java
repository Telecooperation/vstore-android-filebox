package vstore.android_filebox.files_fragment;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import vstore.framework.file.VStoreFile;

public class ListWithDateHeaders<T> extends ArrayList {
    private boolean mSortDirection;

    /**
     * Constructs a new self-sorting element list which also adds date headers
     * between the file objects
     * @param sortDirection true = Newest first, false = Oldest first
     */
    public ListWithDateHeaders(boolean sortDirection) {
        super();
        mSortDirection = sortDirection;
    }

    /**
     * Sets the sort direction for the list.
     * @param newestFirst True = newest first, False = oldest first.
     */
    public void setSortDirection(boolean newestFirst) {
        mSortDirection = newestFirst;
    }

    /**
     * Adds the given list of elements into this ListWithDateHeaders object.
     * Each element will be inserted using addElement(VStorageFile).
     * @param elements The elements to insert
     */
    public void addElements(List<VStoreFile> elements) {
        for(VStoreFile f : elements) {
            if(f != null) {
                addElement(f);
            }
        }
    }

    /**
     * This function adds an element at the correct position in the list
     * according to the lists sort direction. It also adds new date headers if needed.
     * @param f The file to be inserted into the list.
     */
    public void addElement(VStoreFile f) {
        if(this.size() > 0) {
            //Insert newest at the front
            //Start to check from the beginning of the list
            int i;
            for (i = 0; i < this.size(); i++) {
                Object o1 = this.get(i);
                if(o1 instanceof DateHeader) {
                    DateHeader h = (DateHeader) o1;
                    if(
                            //For direction "newest first"
                            (mSortDirection && h.getDate().before(f.getCreationDate())
                            && isSameMonthLowerDay(h.getDate(), f.getCreationDate()))

                    ||
                            //For direction "oldest first"
                            (!mSortDirection && h.getDate().after(f.getCreationDate())
                                    && isSameMonthHigherDay(h.getDate(), f.getCreationDate())))
                    {
                        //We need to insert here with new header
                        this.addNewHeader(i, f.getCreationDate());
                        this.add(i+1, f);
                        return;
                    } else if(isSameDay(h.getDate(), f.getCreationDate())) {
                        //We need to insert under this header. But at the right position
                        //concerning the hours, minutes and seconds
                        int j;
                        for(j = i+1; j<this.size(); ++j) {
                            Object o2 = this.get(j);
                            if(o2 instanceof VStoreFile) {
                                VStoreFile listFile = (VStoreFile) o2;
                                if(
                                    //For direction "newest first"
                                    (mSortDirection && listFile.getCreationDate().before(f.getCreationDate()))
                                ||
                                    //For direction "oldest first"
                                    (!mSortDirection && listFile.getCreationDate().after(f.getCreationDate())))
                                {
                                    //We need to insert before listFile
                                    this.add(j, f);
                                    return;
                                }
                            } else if(o2 instanceof DateHeader) {
                                //We reached the end of this "subarea" for the current header.
                                //Thus, we need to insert here, as last element of this subarea,
                                //before the next header.
                                this.add(j, f);
                                return;
                            }
                        }
                        //When the second for loop reached here, it did not insert yet in the
                        //subarea. Thus, we need to insert here
                        this.add(j, f);
                        return;
                    }
                }
            }
            //When reached here, it did not insert yet and we reached the end of the list.
            this.addNewHeader(this.size(), f.getCreationDate());
            this.add(this.size(), f);
        } else {
            //List is empty, so add first element
            this.addNewHeader(0, f.getCreationDate());
            this.add(f);
        }
    }

    private void addNewHeader(int index, Date d) {
        Format formatter = new SimpleDateFormat("dd.MM.yyyy");
        DateHeader newHeader = new DateHeader(d, formatter.format(d));
        this.add(index, newHeader);
    }

    private boolean isSameMonthLowerDay(Date one, Date two) {
        Calendar c1 = Calendar.getInstance();
        c1.setTime(one);
        Calendar c2 = Calendar.getInstance();
        c2.setTime(two);

        if(c1.get(Calendar.DAY_OF_MONTH) < c2.get(Calendar.DAY_OF_MONTH)
                && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
                && c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR))
        {
            return true;
        } else {
            return false;
        }
    }

    private boolean isSameMonthHigherDay(Date one, Date two) {
        Calendar c1 = Calendar.getInstance();
        c1.setTime(one);
        Calendar c2 = Calendar.getInstance();
        c2.setTime(two);

        if(c1.get(Calendar.DAY_OF_MONTH) > c2.get(Calendar.DAY_OF_MONTH)
                && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
                && c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR))
        {
            return true;
        } else {
            return false;
        }
    }

    private boolean isSameDay(Date one, Date two) {
        Calendar c1 = Calendar.getInstance();
        c1.setTime(one);
        Calendar c2 = Calendar.getInstance();
        c2.setTime(two);

        if(c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH)
                && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
                && c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR))
        {
            return true;
        } else {
            return false;
        }
    }
}
