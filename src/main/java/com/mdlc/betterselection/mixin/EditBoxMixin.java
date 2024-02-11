package com.mdlc.betterselection.mixin;

import com.mdlc.betterselection.WordMachine;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static net.minecraft.client.Minecraft.ON_OSX;


@Mixin(EditBox.class)
public abstract class EditBoxMixin extends AbstractWidget {
    private EditBoxMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Shadow @Final private Font font;
    @Shadow private String value;
    @Shadow private boolean bordered;
    @Shadow private int displayPos;
    @Shadow private long focusedTime;

    @Shadow
    public abstract void moveCursorTo(int i, boolean extend);

    @Shadow
    public abstract int getInnerWidth();

    @Shadow
    public abstract void moveCursorToEnd(boolean extend);

    @Shadow
    public abstract void moveCursorToStart(boolean extend);

    @Shadow
    public abstract void setValue(String string);

    @Shadow
    public abstract String getValue();

    @Shadow
    public abstract int getCursorPosition();

    /**
     * Traverses one or multiple words.
     *
     * @param directedCount
     *         the number of words to traverse; negative to the left, positive to the right
     * @param index
     *         the initial position of the cursor
     * @return the index of the position of the cursor after traversing the words
     */
    @Unique
    private int traverseWord(int directedCount, int index) {
        int direction = directedCount > 0 ? 1 : -1;
        int readOffset = directedCount > 0 ? 0 : -1;
        int count = Mth.abs(directedCount);

        try {
            for (int i = 0; i < count; i++) {
                // Words are separated by at least one whitespace
                if (WordMachine.isWhitespaceCharacter(this.value.charAt(index + readOffset))) {
                    index += direction;
                }

                WordMachine machine = WordMachine.startRead(this.value.charAt(index + readOffset));
                while (machine != WordMachine.DONE) {
                    index += direction;
                    machine = machine.read(this.value.charAt(index + readOffset));
                }
            }
            return index;
        } catch (IndexOutOfBoundsException exception) {
            return index <= 0 ? 0 : this.value.length();
        }
    }

    /**
     * Improves the vanilla word-by-word cursor movement feature.
     */
    @Inject(method = "getWordPosition(IIZ)I", at = @At("HEAD"), cancellable = true)
    private void onGetWordPosition(int directedCount, int index, boolean skipWhitespaceAfterWord, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(traverseWord(directedCount, index));
    }

    /**
     * When the cursor moves, restarts flickering animation to make sure it is displayed.
     */
    @Inject(method = "setCursorPosition", at = @At("HEAD"))
    private void onSetCursorPosition(int position, CallbackInfo ci) {
        this.focusedTime = Util.getMillis();
    }

    /**
     * Computes the index of the character boundary whose abscissa is closest to {@code x} in {@code text} for
     * {@code font}.
     */
    @Unique
    private static int nearestCharacterBoundary(Font font, String text, int x) {
        // The prefix ends before clicked character
        String prefix = font.plainSubstrByWidth(text, x);
        int clickedCharacterIndex = prefix.length();
        if (clickedCharacterIndex >= text.length()) {
            return clickedCharacterIndex;
        } else {
            int prefixWidth = font.width(prefix);
            int clickedCharacterWidth = font.width(String.valueOf(text.charAt(clickedCharacterIndex)));
            int spaceLeft = x - prefixWidth;
            int spaceRight = prefixWidth + clickedCharacterWidth - x;
            if (spaceRight > spaceLeft) {
                return clickedCharacterIndex;
            } else {
                return clickedCharacterIndex + 1;
            }
        }
    }

    /**
     * Makes mouse selection more precise.
     */
    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(method = "onClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;moveCursorTo(IZ)V"), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void moveCursorCloserToMouse(double x, double y, CallbackInfo ci, int mouseXInBox, String displayedText) {
        this.moveCursorTo(nearestCharacterBoundary(this.font, displayedText, mouseXInBox), Screen.hasShiftDown());
        ci.cancel();
    }

    /**
     * Enables text selection by dragging the mouse.
     */
    @Override
    protected void onDrag(double x, double y, double deltaX, double deltaY) {
        int mouseXInBox = Mth.floor(x) - this.getX();
        if (this.bordered) {
            mouseXInBox -= 4;
        }
        String displayedText = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), this.getInnerWidth());
        this.moveCursorTo(nearestCharacterBoundary(this.font, displayedText, mouseXInBox), true);
        super.onDrag(x, y, deltaX, deltaY);
    }

    /**
     * On macOS, use Option + arrow to navigate by word instead of Command + arrow, as is standard on macOS.
     */
    @Redirect(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;hasControlDown()Z"))
    private boolean onHasControlDown() {
        return ON_OSX ? Screen.hasAltDown() : Screen.hasControlDown();
    }

    /**
     * On macOS, use Option + arrow to navigate by word instead of Command + arrow, as is standard on macOS.
     */
    @Redirect(method = "deleteText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;hasControlDown()Z"))
    private boolean onHasControlDownDelete() {
        return ON_OSX ? Screen.hasAltDown() : Screen.hasControlDown();
    }

    /**
     * Handle Command + arrow/backspace/delete on macOS, going to either the start or end.
     */
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void handleMacPresses(int key, int j, int k, CallbackInfoReturnable<Boolean> cir) {
        if (ON_OSX && Screen.hasControlDown()) {
            switch (key) {
                case GLFW.GLFW_KEY_RIGHT -> moveCursorToEnd(Screen.hasShiftDown());
                case GLFW.GLFW_KEY_LEFT -> moveCursorToStart(Screen.hasShiftDown());
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    setValue(getValue().substring(getCursorPosition()));
                    moveCursorToStart(Screen.hasShiftDown());
                }
                case GLFW.GLFW_KEY_DELETE -> {
                    setValue(getValue().substring(0, getCursorPosition()));
                    moveCursorToEnd(Screen.hasShiftDown());
                }
                default -> {
                    return;
                }
            }
            cir.setReturnValue(true);
        }
    }
}
