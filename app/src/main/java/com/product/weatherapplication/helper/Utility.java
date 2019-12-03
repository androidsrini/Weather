package com.product.weatherapplication.helper;

import android.app.ProgressDialog;
import android.content.Context;

import com.product.weatherapplication.R;

public class Utility {
    private static final Utility ourInstance = new Utility();

    public static Utility getInstance() {
        return ourInstance;
    }

    private ProgressDialog progressDialog;

    private Utility() {
    }

    /**
     * This method to create progress dialog object and show dialog.
     * @param context
     */
    public void showProgressDialog(Context context) {
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(context.getString(R.string.loading));
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(false);
        progressDialog.show();
    }

    /**
     * This method to hide loading progress bar
     */
    public void dismissDialog() {
        if (null != progressDialog && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
