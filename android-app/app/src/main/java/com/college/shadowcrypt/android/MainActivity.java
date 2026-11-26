package com.college.shadowcrypt.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.EditText;

/**
 * Pure Java Android Activity hosting ShadowCrypt in fullscreen landscape mode.
 */
public final class MainActivity extends Activity {

    private GameSurfaceView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameView = new GameSurfaceView(this, this::showNameDialog);
        setContentView(gameView);
        hideSystemBars();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBars();
        if (gameView != null) {
            gameView.onActivityResumed();
        }
    }

    @Override
    protected void onPause() {
        if (gameView != null) {
            gameView.onActivityPaused();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (gameView != null) {
            gameView.onActivityDestroyed();
        }
        super.onDestroy();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (gameView != null && gameView.handleBackPress()) {
            return;
        }
        super.onBackPressed();
    }

    private void showNameDialog(String currentName) {
        runOnUiThread(() -> {
            EditText edit = new EditText(this);
            edit.setText(currentName);
            edit.setSingleLine(true);
            edit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16)});
            edit.setSelection(edit.getText().length());

            new AlertDialog.Builder(this)
                    .setTitle("Adventurer Name")
                    .setView(edit)
                    .setPositiveButton("Save", (d, w) -> gameView.setPlayerName(edit.getText().toString()))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @SuppressWarnings("deprecation")
    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }
}
