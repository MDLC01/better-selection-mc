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
import org.spongepowered.asm.mixin.injection.Redirect;
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
    @Shadow private int displayPos;

    @Shadow
    public abstract void moveCursorTo(int i);


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
     * Tests if the shift key is down instead directly instead of using a cached value that might not be up-to-date.
     * <p>
     * Fixes <a href="https://bugs.mojang.com/browse/MC-260563">MC-260563</a>.
     */
    @Redirect(method = "moveCursorTo", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/EditBox;shiftPressed:Z"))
    private boolean redirectShiftPressed(EditBox instance) {
        return Screen.hasShiftDown();
    }

    /**
     * When the cursor moves, restart flickering animation to make sure it is displayed.
     */
    @Inject(method = "setCursorPosition", at = @At("HEAD"))
    private void onSetCursorPosition(int position, CallbackInfo ci) {
        this.frame = 0;
    }

    /**
     * When the mouse is clicked over a character, moves the cursor to the closest possible position, instead of always
     * after the clicked character.
     */
    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(method = "onClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;moveCursorTo(I)V"), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void moveCursorCloserToMouse(double x, double y, CallbackInfo ci, int mouseXInBox, String displayedText) {
        // The prefix is the text between the beginning of the box and the clicked character (excluded)
        String prefix = this.font.plainSubstrByWidth(displayedText, mouseXInBox);
        int clickedCharacterIndex = this.displayPos + prefix.length();
        if (clickedCharacterIndex == this.value.length()) {
            this.moveCursorTo(clickedCharacterIndex);
        } else {
            int prefixWidth = this.font.width(prefix);
            int clickedCharacterWidth = this.font.width(displayedText.substring(prefix.length(), prefix.length() + 1));
            int spaceLeft = mouseXInBox - prefixWidth;
            int spaceRight = prefixWidth + clickedCharacterWidth - mouseXInBox;
            if (spaceRight > spaceLeft) {
                this.moveCursorTo(clickedCharacterIndex);
            } else {
                this.moveCursorTo(clickedCharacterIndex + 1);
            }
        }
        ci.cancel();
    }
}
