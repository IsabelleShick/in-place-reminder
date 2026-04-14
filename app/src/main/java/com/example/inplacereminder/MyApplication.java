package com.example.inplacereminder;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class MyApplication extends Application {
    private volatile long currentUserId = -1;
    private volatile byte[] currentUserPicture = null;

    public synchronized void setCurrentUserId(long id) {
        this.currentUserId = id;
    }

    public synchronized long getCurrentUserId() {
        return currentUserId;
    }

    public synchronized void setCurrentUserPictureBytes(byte[] bytes) {
        this.currentUserPicture = bytes;
    }

    public synchronized byte[] getCurrentUserPictureBytes() {
        return currentUserPicture;
    }

    // convenience: decode bytes to Bitmap (null-safe)
    public Bitmap getCurrentUserBitmap() {
        byte[] b;
        synchronized (this) {
            b = currentUserPicture;
        }
        if (b == null || b.length == 0) return null;
        return BitmapFactory.decodeByteArray(b, 0, b.length);
    }

    // clear on logout
    public synchronized void clearCurrentUser() {
        this.currentUserId = -1;
        this.currentUserPicture = null;
    }
}
