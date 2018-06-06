package vstore.android_filebox.context_fragment;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.google.android.gms.maps.MapView;

/**
 * This class extends the default Google Map View (com.google.android.gms.maps.MapView)
 * but overrides the dispatchTouchEvent Listener so that scrolling through the map is possible
 * although the map is in a scrollview.
 */
public class CustomMapView extends MapView {
    public CustomMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_UP:
                this.getParent().requestDisallowInterceptTouchEvent(false);
                break;
            case MotionEvent.ACTION_DOWN:
                this.getParent().requestDisallowInterceptTouchEvent(true);
                break;
        }
        return super.dispatchTouchEvent(ev);
    }
}
