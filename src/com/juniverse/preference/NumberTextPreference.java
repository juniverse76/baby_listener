//Please note this must be the package if you want to use XML-based preferences
package com.juniverse.preference;
 
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.juniverse.babylistener.R;

/**
 * A preference type that allows a user to choose a time
 */
public class NumberTextPreference extends DialogPreference
{
	private View mView;
	/**
	 * @param context
	 * @param attrs
	 */
	public NumberTextPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}
 
	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public NumberTextPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize();
	}
 
	/**
	 * Initialize this preference
	 */
	private void initialize() {
		setPersistent(true);
	}
 
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if ( positiveResult )
		{
			EditText value = (EditText) mView.findViewById(R.id.s_value);
			String newValue = value.getText().toString();
			if ( newValue != null && callChangeListener(newValue) )
			{
		        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
	            SharedPreferences.Editor editor = pref.edit();
	            editor.putString(getKey(), newValue);
	            editor.commit();
			}
		}
		
		super.onDialogClosed(positiveResult);
	}

	@Override
	protected View onCreateDialogView()
	{
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mView = inflater.inflate(R.layout.setting_number_only, null);
		
		return mView;
	}

	@Override
	protected void onBindDialogView(View view) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        EditText value = (EditText) mView.findViewById(R.id.s_value);
        value.setText(pref.getString(getKey(), "0"));
        
		super.onBindDialogView(view);
	}
}