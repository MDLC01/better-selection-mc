package com.mdlc.betterselection.mixin;

import com.mdlc.betterselection.CharacterClass;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;


@Mixin(EditBox.class)
public abstract class EditBoxMixin extends AbstractWidget {
    private EditBoxMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Shadow @Final private Font font;
    @Shadow private String value;
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

        if (index == 0 && direction == -1 || index == this.value.length() && direction == 1) {
            return index;
        }

        for (int i = 0; i < count; i++) {
            // Words are separated by at least one whitespace
            if (Character.isSpaceChar(this.value.charAt(index + readOffset))) {
                index += direction;
            }
            if (index == 0 && direction == -1 || index == this.value.length() && direction == 1) {
                return index;
            }

            CharacterClass characterClass = CharacterClass.fromCharacter(this.value.charAt(index + readOffset));
            do {
                index += direction;
                if (index == 0 && direction == -1 || index == this.value.length() && direction == 1) {
                    return index;
                }
            } while (characterClass.contains(this.value.charAt(index + readOffset)));
        }

        return index;
    }

    /**
     * Improves the Vanilla word-by-word cursor movement feature.
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
     * When the mouse is clicked over a character, moves the cursor to the closest possible position, instead of always
     * after the clicked character.
     */
    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;moveCursorTo(I)V"), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void moveCursorCloserToMouse(double x, double y, int button, CallbackInfoReturnable<Boolean> cir, boolean isClickWithinBox, int mouseXInBox, String displayedText) {
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
        cir.setReturnValue(true);
    }
}
