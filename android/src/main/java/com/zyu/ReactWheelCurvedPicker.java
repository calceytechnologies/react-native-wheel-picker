package com.zyu;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;


import com.aigestudio.wheelpicker.view.WheelCurvedPicker;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.EventDispatcher;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.graphics.Rect;
import android.view.View;
import android.text.TextUtils;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:lesliesam@hotmail.com"> Sam Yu </a>
 */
public class ReactWheelCurvedPicker extends WheelCurvedPicker {

    private final EventDispatcher mEventDispatcher;
    private List<Integer> mValueData;
	
    private Integer mLineColor = Color.WHITE; // Default line color
    private boolean isLineGradient = false;    // By default line color is not a gradient
    private Integer mLinegradientFrom = Color.BLACK; // Default starting gradient color
    private Integer mLinegradientTo = Color.WHITE; // Default end gradient color

    /** The instance of the node provider for the virtual tree - lazily instantiated. */
    private AccessibilityNodeProvider mAccessibilityNodeProvider; 

    public ReactWheelCurvedPicker(ReactContext reactContext) {
        super(reactContext);
        mEventDispatcher = reactContext.getNativeModule(UIManagerModule.class).getEventDispatcher();
        setOnWheelChangeListener(new OnWheelChangeListener() {
            @Override
            public void onWheelScrolling(float deltaX, float deltaY) {
            }

            @Override
            public void onWheelSelected(int index, String data) {
                if (mValueData != null && index < mValueData.size()) {
                    mEventDispatcher.dispatchEvent(
                            new ItemSelectedEvent(getId(), mValueData.get(index)));
                }
            }

            @Override
            public void onWheelScrollStateChanged(int state) {
            }
        });
    }

    @Override
    public void onInitializeAccessibilityNodeInfo (AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);

        Rect r = new Rect();
        boolean isVisible = this.getGlobalVisibleRect(r);
        info.setBoundsInScreen(r);
        info.setVisibleToUser(isVisible);
        info.setClassName(this.getClass().getCanonicalName());
        info.setPackageName(getContext().getPackageName());
        info.setScrollable(isVisible);
        info.setEnabled(true);
        info.setText(data.get(itemIndex));
    }

    @Override
    public AccessibilityNodeProvider getAccessibilityNodeProvider() {
        if (mAccessibilityNodeProvider == null) {
            mAccessibilityNodeProvider = new VirtualDescendantsProvider();
        }
        return mAccessibilityNodeProvider;
    }           

    @Override
    protected void drawForeground(Canvas canvas) {
        super.drawForeground(canvas);

        Paint paint = new Paint();
        paint.setColor(Color.BLACK); // changed this from WHITE to BLACK
	    
	if (this.isLineGradient) {
	  int colorFrom = this.mLinegradientFrom;
	  int colorTo = this.mLinegradientTo;

	  LinearGradient linearGradientShader = new LinearGradient(rectCurItem.left, rectCurItem.top, rectCurItem.right/2, rectCurItem.top, colorFrom, colorTo, Shader.TileMode.MIRROR);
	  paint.setShader(linearGradientShader);
        }
	    
        canvas.drawLine(rectCurItem.left, rectCurItem.top, rectCurItem.right, rectCurItem.top, paint);
        canvas.drawLine(rectCurItem.left, rectCurItem.bottom, rectCurItem.right, rectCurItem.bottom, paint);
    }
	
    public void setLineColor(Integer color) {
        this.mLineColor = color;
    }

    public void setLineGradientColorFrom (Integer color) {
        this.isLineGradient = true;
        this.mLinegradientFrom = color;
    }

    public void setLineGradientColorTo (Integer color) {
        this.isLineGradient = true;
        this.mLinegradientTo = color;
    }

    @Override
    public void setItemIndex(int index) {
        super.setItemIndex(index);
        unitDeltaTotal = 0;
		mHandler.post(this);
    }

    public void setValueData(List<Integer> data) {
        mValueData = data;
    }

    public int getState() {
        return state;
    }

    private class VirtualDescendantsProvider extends AccessibilityNodeProvider {
        @Override
        public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
            AccessibilityNodeInfo info = null;
            ReactWheelCurvedPicker root = ReactWheelCurvedPicker.this;

            if (virtualViewId == View.NO_ID) {
                info = AccessibilityNodeInfo.obtain(root);
                onInitializeAccessibilityNodeInfo(info);

                int childCount = root.mValueData != null ? root.mValueData.size() : 0;
                int sideSize = (root.itemCount % 2 == 0  ? root.itemCount : (root.itemCount - 1))/2;
                int minIndex = Math.max(root.itemIndex - sideSize,0);
                int maxIndex = Math.min(root.itemIndex + sideSize, childCount-1);

                for (int i = minIndex; i <= maxIndex; ++i) {
                    info.addChild(root, i);
                }
            } else {
                info = AccessibilityNodeInfo.obtain();
                info.setClassName(root.getClass().getName() + "Item");
                info.setPackageName(getContext().getPackageName());
                info.setSource(root, virtualViewId);

                // A Naive computation of bounds per item, by dividing global space
                // to slots per itemsCount, and figuring out the right position
                // as offset from the selected item, which is the center
                int childCount = root.mValueData != null ? root.mValueData.size() : 0;
                int sideSize = (root.itemCount % 2 == 0  ? root.itemCount : (root.itemCount - 1))/2;
                int minIndex = Math.max(root.itemIndex - sideSize,0);
                int maxIndex = Math.min(root.itemIndex + sideSize, childCount-1);
                boolean isInView = (virtualViewId>= minIndex && virtualViewId<=maxIndex);

                Rect r = new Rect();
                boolean isVisible = isInView && root.getGlobalVisibleRect(r);

                if(isInView) {
                    int itemHeight = r.height()/root.itemCount;
                    r.top += (virtualViewId - root.itemIndex + sideSize)*itemHeight;
                    r.bottom = r.top + itemHeight;
                }
                else {
                    r.top = r.bottom = r.left = r.right = 0;
                }

                info.setBoundsInScreen(r);
                info.setVisibleToUser(isVisible);
                info.setParent(root);
                info.setText(root.data.get(virtualViewId));
                info.setSelected(root.itemIndex == virtualViewId);
                info.setEnabled(true);
            }
            return info;
        }

        @Override
        public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(String searched,
                                                                            int virtualViewId) {
            if (TextUtils.isEmpty(searched)) {
                return Collections.emptyList();
            }
            String searchedLowerCase = searched.toLowerCase();
            List<AccessibilityNodeInfo> result = null;
            ReactWheelCurvedPicker root = ReactWheelCurvedPicker.this;

            if (virtualViewId == View.NO_ID) {
                for (int i = 0; i < root.data.size(); i++) {
                    String textToLowerCase = root.data.get(i).toLowerCase();
                    if (textToLowerCase.contains(searchedLowerCase)) {
                        if (result == null) {
                            result = new ArrayList<AccessibilityNodeInfo>();
                        }
                        result.add(createAccessibilityNodeInfo(i));
                    }
                }
            } else {
                String textToLowerCase = root.data.get(virtualViewId).toLowerCase();
                if (textToLowerCase.contains(searchedLowerCase)) {
                    result = new ArrayList<AccessibilityNodeInfo>();
                    result.add(createAccessibilityNodeInfo(virtualViewId));
                }
            }
            if (result == null) {
                return Collections.emptyList();
            }
            return result;
        }

        @Override
        public boolean performAction(int virtualViewId, int action, Bundle arguments) {

            ReactWheelCurvedPicker root = ReactWheelCurvedPicker.this;

            if (virtualViewId == View.NO_ID) {
                switch (action) {
                    // Allowing lowercase search as in the implementation of findAccessibilityNodeInfosByText
                    case AccessibilityNodeInfo.ACTION_SET_TEXT:
                        CharSequence chars = arguments.getCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE);
                        String searched = chars.toString();
                        if(TextUtils.isEmpty(searched))
                            return true; // ignore empty text
                        String searchedLowerCase = searched.toLowerCase();
                        for (int i = 0; i < root.data.size(); i++) {
                            String textToLowerCase = root.data.get(i).toLowerCase();
                            if (textToLowerCase.contains(searchedLowerCase)) {
                                root.setItemIndex(i);
                                break;
                            }
                        }
                        return true;
                }
            }
            return false;
        }
    }  
}

class ItemSelectedEvent extends Event<ItemSelectedEvent> {

    public static final String EVENT_NAME = "wheelCurvedPickerPageSelected";

    private final int mValue;

    protected ItemSelectedEvent(int viewTag,  int value) {
        super(viewTag);
        mValue = value;
    }

    @Override
    public String getEventName() {
        return EVENT_NAME;
    }

    @Override
    public void dispatch(RCTEventEmitter rctEventEmitter) {
        rctEventEmitter.receiveEvent(getViewTag(), getEventName(), serializeEventData());
    }

    private WritableMap serializeEventData() {
        WritableMap eventData = Arguments.createMap();
        eventData.putInt("data", mValue);
        return eventData;
    }
}
