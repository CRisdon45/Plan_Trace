import android.view.InputDevice;
import android.view.InputEvent;
import android.view.MotionEvent;
import android.os.SystemClock;
import java.lang.reflect.Method;

public class PenGesture {
    static Object manager;
    static Method inject;
    static long down;
    static void send(int action, float x, float y, int buttons, boolean palm, int flags) throws Exception {
        int count = palm ? 2 : 1;
        MotionEvent.PointerProperties[] props = new MotionEvent.PointerProperties[count];
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[count];
        for (int i = 0; i < count; i++) {
            props[i] = new MotionEvent.PointerProperties(); props[i].id = i;
            props[i].toolType = i == 0 ? MotionEvent.TOOL_TYPE_STYLUS : MotionEvent.TOOL_TYPE_FINGER;
            coords[i] = new MotionEvent.PointerCoords(); coords[i].x = i == 0 ? x : 1800;
            coords[i].y = i == 0 ? y : 1100; coords[i].pressure = .7f; coords[i].size = .05f;
        }
        MotionEvent event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, count, props, coords,
            0, buttons, 1, 1, 0, 0, InputDevice.SOURCE_STYLUS, flags);
        if (!((Boolean) inject.invoke(manager, event, 2))) throw new RuntimeException("Injection rejected");
        event.recycle();
    }
    public static void main(String[] args) throws Exception {
        Class<?> cls = Class.forName("android.hardware.input.InputManagerGlobal");
        manager = cls.getMethod("getInstance").invoke(null);
        inject = cls.getMethod("injectInputEvent", InputEvent.class, int.class);
        String mode = args[0];
        float x1 = Float.parseFloat(args[1]), y1 = Float.parseFloat(args[2]);
        float x2 = Float.parseFloat(args[3]), y2 = Float.parseFloat(args[4]);
        boolean held = mode.equals("held") || mode.equals("held-release") || mode.equals("focus-loss");
        boolean palm = mode.equals("palm");
        down = SystemClock.uptimeMillis();
        send(MotionEvent.ACTION_DOWN, x1, y1, held ? MotionEvent.BUTTON_STYLUS_PRIMARY : 0, false, 0);
        if (palm) send(MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT), x1, y1, 0, true, 0);
        for (int i = 1; i <= 20; i++) {
            SystemClock.sleep(30);
            boolean button = mode.equals("held") || mode.equals("focus-loss") || (mode.equals("held-release") && i <= 10) || (mode.equals("late-press") && i > 10);
            send(MotionEvent.ACTION_MOVE, x1 + (x2-x1)*i/20f, y1 + (y2-y1)*i/20f,
                button ? MotionEvent.BUTTON_STYLUS_PRIMARY : 0, palm, 0);
        }
        if (mode.equals("focus-loss")) {
            Runtime.getRuntime().exec(new String[]{"input", "keyevent", "3"}).waitFor();
            System.out.println("Synthetic pen interrupted by Home");
            return;
        }
        if (palm) send(MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT), x2, y2, 0, true, 0);
        send(mode.equals("cancel") ? MotionEvent.ACTION_CANCEL : MotionEvent.ACTION_UP, x2, y2,
            mode.equals("held") || mode.equals("late-press") ? MotionEvent.BUTTON_STYLUS_PRIMARY : 0,
            false, mode.equals("rejected") ? MotionEvent.FLAG_CANCELED : 0);
        System.out.println("Synthetic pen sequence completed: " + mode);
    }
}
