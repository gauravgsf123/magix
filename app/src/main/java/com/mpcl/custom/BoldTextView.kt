package com.mpcl.custom

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView


class BoldTextView(context: Context, attrs: AttributeSet?) : AppCompatTextView(context, attrs) {
    init {
        val typeface = Typeface.createFromAsset(getContext().assets, "font/kameron_bold.ttf")
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB ||
            android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.HONEYCOMB_MR2) {
            setTypeface(typeface)
            /*if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M){
                setTextColor(ContextCompat.getColor(context, R.color.black));
            }else
                setTextColor(getResources().getColor(R.color.black));*/
        }
    }
}