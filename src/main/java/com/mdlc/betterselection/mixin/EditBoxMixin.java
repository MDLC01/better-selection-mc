package com.mdlc.betterselection.mixin;

import com.mdlc.betterselection.WordMachine;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;


@Mixin(EditBox.class)
public abstract class EditBoxMixin extends AbstractWidget {
    private EditBoxMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Shadow @Final private Font font;
    @Shadow private String value;
    @Shadow private int frame;
    @Shadow private boolean bordered;
    @Shadow private boolean shiftPressed;
    @Shadow private int displayPos;

    @Shadow
    public abstract void moveCursorTo(int i);

    @Shadow
    public abstract int getInnerWidth();

    /**
     * Traverses one or multiple words.
     *
     * @param directedCount
     *         the number of words to traverse; negative to the left, positive to the right
     * @param index
     *         the initial position of the cursor
     * @return the index of the position of the cursor after traversing the words
     */
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
        this.frame = 0;
    }

    /**
     * Computes the index of the character boundary whose abscissa is closest to {@code x} in {@code text} for
     * {@code font}.
     */
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
     * Makes mouse selection more precise and fix <a href="https://bugs.mojang.com/browse/MC-260563">MC-260563</a>.
     */
    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(method = "onClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;moveCursorTo(I)V"), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void moveCursorCloserToMouse(double x, double y, CallbackInfo ci, int mouseXInBox, String displayedText) {
        this.shiftPressed = Screen.hasShiftDown();
        this.moveCursorTo(nearestCharacterBoundary(this.font, displayedText, mouseXInBox));
        ci.cancel();
    }

    /**
     * Enables text selection by dragging the mouse.
     */
    @Override
    protected void onDrag(double x, double y, double deltaX, double deltaY) {
        this.shiftPressed = true;
        int mouseXInBox = Mth.floor(x) - this.getX();
        if (this.bordered) {
            mouseXInBox -= 4;
        }
        String displayedText = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), this.getInnerWidth());
        this.moveCursorTo(nearestCharacterBoundary(this.font, displayedText, mouseXInBox));
        super.onDrag(x, y, deltaX, deltaY);
    }
}
