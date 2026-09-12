# Pen button and contact integrity

## Interaction

Default: hold the primary barrel button before pen contact to temporarily select or move an editable object. Releasing the button during contact leaves that gesture in Select until pen-up. The next unmodified contact uses the previously chosen drawing tool and settings. Pressing the button during an ink contact does not convert that stroke; the modifier is sampled at the next contact.

More Options offers Select and Erase objects button assignments, saved with drawing preferences. Temporary erasing uses the existing object-outline eraser and respects layer locks. It is not stroke-segment erasing. A visible temporary-mode label appears during modified contacts. The default no longer interprets double-clicks as Undo or opens the old radial palette.

Calibration takes priority over both the modifier and hardware eraser. Cancellation and focus loss discard the active gesture and clear temporary state. Selection ignores hidden or locked layers. Pen input is routed by the actual stylus pointer index, so additional fingers do not automatically convert a pen stroke into pinch navigation. Remaining fingers after pen-up are suppressed until released. Finger-only navigation remains separate.

The contact state machine and routing follow the event model described in [Android's stylus guidance](https://developer.android.com/develop/ui/views/touch-and-input/stylus-input/advanced-stylus-features). This implementation handles system cancellation and rejected pen-up events. On this Compose/device path, an injected FLAG_CANCELED reached Activity.dispatchTouchEvent but did not reach the canvas callback. The app now converts a rejected contact to ACTION_CANCEL before Compose dispatch, without mutating the incoming event. An unrelated rejected finger-up must not cancel the active pen.

## Scope and evidence limits

These changes implement temporary single-object selection/movement and object erasing. The broader vision's lasso, multi-selection, segment eraser, configurable Pan/Quick Tools, hover setup check, pressure/tilt tuning and polished inspector remain future work. Physical button timing, hand comfort, hardware palm rejection and device-specific hover behavior require hands-on validation. Synthetic MotionEvent sequences exercise the real installed app but do not certify those physical qualities.

Automated and tablet results are recorded below after final verification.

## Verified results

**30 tests passed, zero failures**: 7 pen-contact/routing tests, 5 document/migration tests, 10 integrity tests and 8 existing model tests. Debug assembly succeeded. The final installed APK SHA-256 is `1E949A6851234DACFAE1A70EE6A4C2B636EA8B08336F119755B93021D103F1D7`.

Controlled synthetic sequences were injected through Android's input system into the installed Plan Trace Dev app on Samsung Tab S10 FE, Android 16, in a separate QA_Pen_Contract project:

- Held button, dragged a rectangle, released the button halfway through contact: the rectangle translated exactly 200 by 100 world pixels; object count stayed one. One Undo restored its exact original bounds.
- The next unmodified pen contact added one freehand path. Pressing the button halfway through another ink contact produced one continuous path, without converting or deleting it.
- ACTION_CANCEL added no object and preserved the prior geometry exactly.
- Initial FLAG_CANCELED testing exposed the framework-boundary problem described above. After correction, the same injected rejected pen-up retained all baseline objects exactly on an unlocked layer. The initial failure is not counted as a pass.
- A stationary finger joined and left a moving pen contact: the final path ran from (1200,849) to (1600,949), with all intermediate points following that world-space trajectory. It did not pan/zoom or lose the stroke.
- Temporary selection on a locked layer left geometry unchanged.
- Temporary object erase over the rectangle outline changed the count from five to four. Undo restored all five exact records. An earlier attempt through the hollow rectangle interior touched no outline and correctly erased nothing; that attempt is not used as evidence of successful erasing.
- Calibration with the erase modifier held opened the scale flow and Cancel retained geometry exactly on an unlocked layer.
- The Erase objects assignment survived reinstall/restart. The normal pen resumed afterward and added one path; it was undone. The assignment was returned to Select.
- A temporary move interrupted by Home was cancelled. Returning to the app restored the exact pre-gesture geometry.

Local raw evidence is in `Plan_Trace-Foundation-QA/pen-*`. The synthetic helper source is preserved at `tools/qa/PenGesture.java`; it supports normal, held, held-release, late-press, cancel, rejected, palm and focus-loss sequences. Build it with the local JDK and Android SDK, convert with d8, and invoke it through app_process only against a disposable test project. Physical hardware validation remains outstanding as described above; no system pen settings were changed.
