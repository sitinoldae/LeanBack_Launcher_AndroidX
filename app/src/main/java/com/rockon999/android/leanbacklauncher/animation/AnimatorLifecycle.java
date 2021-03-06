package com.rockon999.android.leanbacklauncher.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.rockon999.android.leanbacklauncher.R;
import com.rockon999.android.leanbacklauncher.util.Preconditions;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

public final class AnimatorLifecycle implements Resettable, Joinable {
    private static final String TAG = "AnimatorLifecycle";
    public final Rect lastKnownEpicenter;
    private Animator mAnimation;
    private Runnable mCallback;
    private byte mFlags;
    private final Handler mHandler;
    private OnAnimationFinishedListener mOnAnimationFinishedListener;
    private final ArrayList<String> mRecentAnimationDumps;

    public interface OnAnimationFinishedListener {
        void onAnimationFinished();
    }

    /* renamed from: AnimatorLifecycle.1 */
    class C01751 extends Handler {
        C01751() {
        }

        @SuppressLint("PrivateResource")
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case R.styleable.RecyclerView_android_descendantFocusability /*1*/:
                    if (AnimatorLifecycle.this.isPrimed()) {
                        AnimatorLifecycle.this.start();
                    }
                default:
            }
        }
    }

    /* renamed from: AnimatorLifecycle.2 */
    class C01762 extends AnimatorListenerAdapter {
        private boolean mCancelled;

        C01762() {
        }

        public void onAnimationCancel(Animator animation) {
            this.mCancelled = true;
        }

        public void onAnimationEnd(Animator animation) {
            animation.removeListener(this);
            AnimatorLifecycle.this.setState((byte) 16);
            if (AnimatorLifecycle.this.mAnimation == null) {
                StringWriter buf = new StringWriter();
                PrintWriter writer = new PrintWriter(buf);
                writer.println("listener notified of animation end when mAnimation==null");
                new Exception("stack trace").printStackTrace(writer);
                AnimatorLifecycle.this.dump("", writer, null);
                writer.println(animation.toString());
                Log.w("Animations", buf.toString());
                ((Resettable) animation).reset();
            }
            if (AnimatorLifecycle.this.mOnAnimationFinishedListener != null) {
                AnimatorLifecycle.this.mOnAnimationFinishedListener.onAnimationFinished();
            }
            if (this.mCancelled) {
                AnimatorLifecycle.this.reset();
            } else if (AnimatorLifecycle.this.mCallback != null && !AnimatorLifecycle.this.mCallback.getClass().getName().equals("com.rockon999.android.leanbacklauncher.MainActivity$C01581")) {
                Log.i(TAG, "AnimatorLifecycle->mCallback->class:" + AnimatorLifecycle.this.mCallback.getClass().getName());
                try {
                    AnimatorLifecycle.this.mCallback.run();
                } catch (Throwable t) {
                    Log.e("Animations", "Could not execute callback", t);
                    AnimatorLifecycle.this.reset();
                }
            }
            if ((AnimatorLifecycle.this.mFlags & 32) != 0) {
                AnimatorLifecycle.this.reset();
            }
        }
    }

    public AnimatorLifecycle() {
        this.lastKnownEpicenter = new Rect();
        this.mHandler = new C01751();
        this.mRecentAnimationDumps = new ArrayList<>();
    }

    public <T extends Animator & Resettable> void init(T animation, Runnable callback, byte flags) {
        if (this.mAnimation != null) {
            StringWriter buf = new StringWriter();
            PrintWriter writer = new PrintWriter(buf);
            writer.println("Called to initialize an animation that was already initialized");
            new Exception("stack trace").printStackTrace(writer);
            dump("", writer, null);
            Log.w("Animations", buf.toString());
            reset();
        }
        this.mAnimation = animation;
        this.mCallback = callback;
        this.mFlags = flags;
        setState((byte) 1);
    }

    public <T extends Animator & Resettable> void schedule() {
        Preconditions.checkState(isInitialized());
        setState((byte) 2);
    }

    public void prime() {
        Preconditions.checkState(isScheduled());
        this.mAnimation.setupStartValues();
        setState((byte) 4);
        this.mHandler.sendEmptyMessageDelayed(1, 1000);
    }

    public void start() {
        boolean isPrimed = (isInitialized() || isScheduled()) || isPrimed();
        Preconditions.checkState(isPrimed);
        this.mAnimation.addListener(new C01762());
        this.mAnimation.start();
        setState((byte) 8);
        if (this.mRecentAnimationDumps != null) {
            while (this.mRecentAnimationDumps.size() >= 10) {
                this.mRecentAnimationDumps.remove(9);
            }
            this.mRecentAnimationDumps.add(0, this.mAnimation.toString());
        }
    }

    public void cancel() {
        if (isRunning()) {
            this.mAnimation.cancel();
        }
    }

    public void reset() {
        cancel();
        if (this.mAnimation != null) {
            ((Resettable) this.mAnimation).reset();
        }
        this.mFlags = (byte) 0;
        this.mAnimation = null;
        this.mCallback = null;
        this.mHandler.removeMessages(1);
    }

    public boolean isInitialized() {
        return !(this.mAnimation == null || (this.mFlags & 1) == 0);
    }

    public boolean isScheduled() {
        return !(this.mAnimation == null || (this.mFlags & 2) == 0);
    }

    public boolean isPrimed() {
        return !(this.mAnimation == null || (this.mFlags & 4) == 0);
    }

    public boolean isRunning() {
        return !(this.mAnimation == null || (this.mFlags & 8) == 0);
    }

    public boolean isFinished() {
        return !(this.mAnimation == null || (this.mFlags & 16) == 0);
    }

    public void setOnAnimationFinishedListener(OnAnimationFinishedListener listener) {
        this.mOnAnimationFinishedListener = listener;
    }

    public void include(View target) {
        if (this.mAnimation instanceof Joinable) {
            ((Joinable) this.mAnimation).include(target);
        }
    }

    public void exclude(View target) {
        if (this.mAnimation instanceof Joinable) {
            ((Joinable) this.mAnimation).exclude(target);
        }
    }

    private void setState(byte state) {
        this.mFlags = (byte) (this.mFlags & -32);
        this.mFlags = (byte) (this.mFlags | state);
        this.mHandler.removeMessages(1);
    }

    @SuppressLint("PrivateResource")
    public void dump(String prefix, PrintWriter writer, ViewGroup root) {
        String str;
        writer.format("%s%s State:\n", prefix, getClass().getSimpleName());
        prefix = prefix + "  ";
        writer.format("%sstate: ", prefix);
        switch (this.mFlags & 31) {
            case R.styleable.RecyclerView_android_descendantFocusability /*1*/:
                writer.write("INIT");
                break;
            case R.styleable.RecyclerView_layoutManager /*2*/:
                writer.write("SCHEDULED");
                break;
            case R.styleable.Preference_android_title /*4*/:
                writer.write("PRIMED");
                break;
            case R.styleable.Preference_android_order /*8*/:
                writer.write("RUNNING");
                break;
            case R.styleable.Preference_order /*16*/:
                writer.write("FINISHED");
                break;
            default:
                writer.write("<idle>");
                break;
        }
        writer.println();
        writer.format("%sflags: ", prefix);
        writer.write((this.mFlags & 32) == 0 ? 46 : 82);
        writer.println();
        writer.format("%slastKnownEpicenter: %d,%d\n", prefix, this.lastKnownEpicenter.centerX(), this.lastKnownEpicenter.centerY());
        String str2 = "%smAnimation: %s\n";
        Object[] objArr = new Object[2];
        objArr[0] = prefix;
        if (this.mAnimation == null) {
            str = "null";
        } else {
            str = this.mAnimation.toString().replaceAll("\n", "\n" + prefix);
        }
        objArr[1] = str;
        writer.format(str2, objArr);
        writer.format("%sAnimatable Views:\n", prefix);
        if (root != null) {
            dumpViewHierarchy(prefix + "  ", writer, root);
        }
        if (this.mRecentAnimationDumps != null) {
            writer.format("%smRecentAnimationDumps: [\n", prefix);
            int n = this.mRecentAnimationDumps.size();
            for (int i = 0; i < n; i++) {
                writer.format("%s    %d) %s\n", prefix, i, ((String) this.mRecentAnimationDumps.get(i)).replaceAll("\n", "\n" + prefix + "    "));
            }
            writer.format("%s]\n", prefix);
        }
    }

    private void dumpViewHierarchy(String prefix, PrintWriter writer, View view) {
        if (view instanceof ParticipatesInLaunchAnimation) {
            writer.format("%s%s\n", prefix, toShortString(view));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            int n = group.getChildCount();
            for (int i = 0; i < n; i++) {
                dumpViewHierarchy(prefix, writer, group.getChildAt(i));
            }
        }
    }

    private String toShortString(View view) {
        char c = '.';
        if (view == null) {
            return "null";
        }
        StringBuilder append = new StringBuilder().append(view.getClass().getSimpleName()).append('@').append(Integer.toHexString(System.identityHashCode(view))).append("{").append(String.format("%.1f", new Object[]{view.getAlpha()})).append(" ").append(String.format("%.1f", new Object[]{view.getTranslationY()})).append(" ").append(String.format("%.1fx%.1f", new Object[]{view.getScaleX(), view.getScaleY()})).append(" ").append(view.isFocused() ? 'F' : '.');
        if (view.isSelected()) {
            c = 'S';
        }
        return append.append(c).append("}").toString();
    }
}
