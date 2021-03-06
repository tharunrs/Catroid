/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2018 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.transfers;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.utils.ToastUtil;
import org.catrobat.catroid.utils.UtilDeviceInfo;
import org.catrobat.catroid.utils.Utils;
import org.catrobat.catroid.web.ServerCalls;
import org.catrobat.catroid.web.WebconnectionException;

public class RegistrationTask extends AsyncTask<Void, Void, Boolean> {

	private static final String TAG = RegistrationTask.class.getSimpleName();

	private Context context;
	private ProgressDialog progressDialog;
	private String username;
	private String password;
	private String email;

	private String message;
	private boolean userRegistered;

	private OnRegistrationListener onRegistrationListener;
	private WebconnectionException exception;

	public RegistrationTask(Context activity, String username, String password, String email) {
		this.context = activity;
		this.username = username;
		this.password = password;
		this.email = email;
	}

	public void setOnRegistrationListener(OnRegistrationListener listener) {
		onRegistrationListener = listener;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if (context == null) {
			return;
		}
		String title = context.getString(R.string.please_wait);
		String message = context.getString(R.string.loading);
		progressDialog = ProgressDialog.show(context, title, message);
	}

	@Override
	protected Boolean doInBackground(Void... arg0) {
		try {
			if (!Utils.isNetworkAvailable(context)) {
				exception = new WebconnectionException(WebconnectionException.ERROR_NETWORK, "Network not available!");
				return false;
			}

			String language = UtilDeviceInfo.getUserLanguageCode();
			String country = UtilDeviceInfo.getUserCountryCode();
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
			String token = sharedPreferences.getString(Constants.TOKEN, Constants.NO_TOKEN);

			userRegistered = ServerCalls.getInstance().register(username, password, email, language,
					country, token, context);

			return true;
		} catch (WebconnectionException webconnectionException) {
			Log.e(TAG, Log.getStackTraceString(webconnectionException));
			message = webconnectionException.getMessage();
		}
		return false;
	}

	@Override
	protected void onPostExecute(Boolean success) {
		super.onPostExecute(success);

		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}

		if (Utils.checkForNetworkError(exception)) {
			ToastUtil.showError(context, R.string.error_internet_connection);
			return;
		}

		if (Utils.checkForSignInError(success, exception, context, userRegistered)) {
			if (message == null) {
				message = context.getString(R.string.register_error);
			}
			onRegistrationListener.onRegistrationFailed(message);
			Log.e(TAG, message);
			return;
		}

		if (userRegistered) {
			ToastUtil.showSuccess(context, R.string.new_user_registered);
		}

		if (onRegistrationListener != null) {
			onRegistrationListener.onRegistrationComplete();
		}
	}

	public interface OnRegistrationListener {

		void onRegistrationComplete();

		void onRegistrationFailed(String msg);
	}
}
