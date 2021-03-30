package com.evandyce.pettinder;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.evandyce.pettinder.main.MainActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;


//https://github.com/firebase/snippets-android/blob/8fa42b206795c271810b038687744b2d2ac15357/auth/app/src/main/java/com/google/firebase/quickstart/auth/EmailPasswordActivity.java#L62-L79

import android.app.Activity;
import android.text.InputType;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.firestore.FirebaseFirestore;
import com.thecode.aestheticdialogs.AestheticDialog;
import com.thecode.aestheticdialogs.DialogAnimation;
import com.thecode.aestheticdialogs.DialogStyle;
import com.thecode.aestheticdialogs.DialogType;
import com.thecode.aestheticdialogs.OnDialogClickListener;

import karpuzoglu.enes.com.fastdialog.Animations;
import karpuzoglu.enes.com.fastdialog.FastDialog;

public class Login extends Activity {

    private static final String TAG = "EmailPassword";
    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private Button mButtCreateAccount;
    private Button mButtLogin;
    private Button mButtResetPassword;

    private EditText mTxtEmail;
    private EditText mTxtPassword;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mButtCreateAccount = (Button)findViewById(R.id.button_create_account);
        mButtLogin = (Button)findViewById(R.id.button_login);
        mButtResetPassword = (Button) findViewById(R.id.button_reset_password);
        mTxtEmail = (EditText)findViewById(R.id.et_email);
        mTxtPassword = (EditText)findViewById(R.id.et_password);

        mButtCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(v.getContext(), SignUp.class));
            }
        });

        mButtLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String em = mTxtEmail.getText().toString();
                String pass = mTxtPassword.getText().toString();

                signIn(em, pass);
            }
        });

        mButtResetPassword.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                EditText resetMail = new EditText(Login.this);
                Drawable style = getResources().getDrawable(R.drawable.custom_input, v.getContext().getTheme());
                resetMail.setBackground(style);
                resetMail.setScaleX(0.9f);
                resetMail.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_email_black, 0, 0, 0);
                resetMail.setCompoundDrawablePadding(20);
                resetMail.setHint("Enter email");
                AlertDialog.Builder passwordResetDialog = new AlertDialog.Builder(v.getContext())
                        .setTitle("Reset Password?")
                        .setMessage("Please enter your email to receive reset link.")
                        .setView(resetMail);

                passwordResetDialog.setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // get email and sent reset through firebase
                        String email = resetMail.getText().toString();
                        mAuth.sendPasswordResetEmail(email)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        popupMessageSuccess("The reset link has been sent.");
                                        Log.d("PasswordResetSuccess", "The email was sent.");
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                String errorMessage = e.getMessage().toString();
                                System.out.println(errorMessage);
                                switch (errorMessage){
                                    case "There is no user record corresponding to this identifier. The user may have been deleted.":
                                        popupMessageFailure("There is no account with this email. Please make an account.");
                                        break;

                                    case "The email address is badly formatted.":
                                        popupMessageFailure("Please enter a valid email address.");
                                        break;
                                }
                            }
                        });
                    }
                });

                passwordResetDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                passwordResetDialog.show();
            }
        });
        // [START initialize_auth]
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        mAuth.setLanguageCode("en");
        // [END initialize_auth]
    }

    // [START on_start_check_user]
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }
    // [END on_start_check_user]

    private void signIn(String email, String password) {
        if (email == null || email.length() == 0 || password == null || password.length() == 0) {
            Toast.makeText(this, "Please enter a valid username and/or password.", Toast.LENGTH_SHORT).show();
            return;
        }

        // [START sign_in_with_email]
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            String errorCode = ((FirebaseAuthException) task.getException()).getErrorCode();

                            switch (errorCode) {

                                case "ERROR_INVALID_CREDENTIAL":
                                    popupMessageFailure("The authentication credential is malformed or expired.");
                                    break;

                                case "ERROR_INVALID_EMAIL":
                                    mTxtEmail.setError("The email address is badly formatted.");
                                    mTxtEmail.requestFocus();
                                    break;

                                case "ERROR_WRONG_PASSWORD":
                                    popupMessageFailure("The password entered is incorrect.");
                                    mTxtPassword.setText("");
                                    break;

                                case "ERROR_USER_MISMATCH":
                                    popupMessageFailure("The supplied credentials do not correspond to the previously signed in user.");
                                    break;

                                case "ERROR_REQUIRES_RECENT_LOGIN":
                                    popupMessageFailure("This operation is sensitive and requires recent authentication. Log in again before retrying this request.");
                                    break;

                                case "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL":
                                    popupMessageFailure("An account already exists with the same email address but different sign-in credentials.");
                                    break;

                                case "ERROR_EMAIL_ALREADY_IN_USE":
                                    mTxtEmail.setError("The email address is already in use by another account.");
                                    mTxtEmail.requestFocus();
                                    break;

                                case "ERROR_CREDENTIAL_ALREADY_IN_USE":
                                    popupMessageFailure("This email is already associated with a different account.");
                                    break;

                                case "ERROR_USER_DISABLED":
                                    popupMessageFailure("This account has been disabled by an administrator");
                                    break;

                                case "ERROR_USER_TOKEN_EXPIRED":
                                    popupMessageFailure("User's credentials have expired. Please sign in again");
                                    break;

                                case "ERROR_USER_NOT_FOUND":
                                    popupMessageFailure("There is no account with this email. Please create an account.");
                                    break;

                                case "ERROR_INVALID_USER_TOKEN":
                                    popupMessageFailure("Please sign in again");
                                    break;

                                case "ERROR_OPERATION_NOT_ALLOWED":
                                    popupMessageFailure("This operation is not allowed.");
                                    break;

                                case "ERROR_WEAK_PASSWORD":
                                    mTxtPassword.setError("The password is invalid it must 6 characters at least");
                                    mTxtPassword.requestFocus();
                                    break;

                            }
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            updateUI(null);
                        }
                    }
                });
        // [END sign_in_with_email]
    }


    private void reload() {
        Intent intent = new Intent(this, MainActivity.class);
        this.startActivity(intent);
    }

    private void updateUI(FirebaseUser user) {
        if (user == null) { return;  }

        this.startActivity(new Intent(this, MainActivity.class));
    }

    public static void logout() {
        FirebaseAuth.getInstance().signOut();
    }

    public void popupMessageFailure(String message){
        new AestheticDialog.Builder(this, DialogStyle.FLAT, DialogType.ERROR)
                .setTitle("Error")
                .setMessage(message)
                .setCancelable(false)
                .setDarkMode(false)
                .setGravity(Gravity.CENTER)
                .setAnimation(DialogAnimation.SHRINK)
                .setOnClickListener(new OnDialogClickListener() {
                    @Override
                    public void onClick(AestheticDialog.Builder builder) {
                        builder.dismiss();
                    }
                })
                .show();
    }

    public void popupMessageSuccess(String message) {
        new AestheticDialog.Builder(this, DialogStyle.FLAT, DialogType.SUCCESS)
                .setTitle("Success")
                .setMessage(message)
                .setCancelable(false)
                .setDarkMode(false)
                .setGravity(Gravity.CENTER)
                .setAnimation(DialogAnimation.SHRINK)
                .setOnClickListener(new OnDialogClickListener() {
                    @Override
                    public void onClick(AestheticDialog.Builder builder) {
                        builder.dismiss();
                    }
                })
                .show();
    }


//            public void onClick(View v) {
//                EditText resetEmail = (EditText)findViewById(R.id.reset_password_email);
//                System.out.println(resetEmail);
//
//                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
//                ViewGroup viewGroup = findViewById(android.R.id.content);
//                View dialogView = LayoutInflater.from(v.getContext()).inflate(R.layout.password_reset_dialog, viewGroup, false);
//                builder.setView(dialogView);
//                builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        String email = resetEmail.getText().toString();
//
//                        mAuth.sendPasswordResetEmail(email)
//                                .addOnSuccessListener(new OnSuccessListener<Void>() {
//                                    @Override
//                                    public void onSuccess(Void aVoid) {
//                                        popupMessageSuccess("The reset email has been sent");
//                                    }
//                                }).addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                String errorMessage = e.getMessage().toString();
//
//                                switch (errorMessage) {
//                                    case "There is no user record corresponding to this identifier. The user may have been deleted.":
//                                        popupMessageFailure("There is no account with this email. Please create an account.");
//                                        break;
//
//                                    case "The email address is badly formatted.":
//                                        popupMessageFailure("Please enter a valid email address.");
//                                        break;
//                                }
//                            }
//                        });
//                    }
//                });
//                AlertDialog alertDialog = builder.create();
}