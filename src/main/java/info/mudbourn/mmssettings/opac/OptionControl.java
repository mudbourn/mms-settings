package info.mudbourn.mmssettings.opac;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

final class OptionControl {
    private static final int ERROR_COLOR = 0xFFFF5555;
    private static final int NORMAL_COLOR = 0xFFE0E0E0;
    private static final int SET_WIDTH = 30;

    private OptionControl() {
    }

    static List<AbstractWidget> create(OptionRef ref, int width, Runnable onFailure) {
        AbstractWidget widget = switch (ref.kind) {
            case BOOLEAN -> toggle(ref, width, onFailure);
            case CYCLE -> cycle(ref, width, onFailure);
            case TEXT -> null;
        };

        if (widget == null) {
            return text(ref, width, onFailure);
        }

        applyCommon(ref, widget);

        return List.of(widget);
    }

    private static void applyCommon(OptionRef ref, AbstractWidget widget) {
        widget.active = widget.active && ref.option.isMutable();

        if (!ref.comment.getString().isEmpty()) {
            widget.setTooltip(Tooltip.create(ref.comment));
        }
    }

    private static Button toggle(OptionRef ref, int width, Runnable onFailure) {
        return Button.builder(boolText(ref.option.getValue()), button -> {
            boolean next = !Boolean.TRUE.equals(ref.option.getValue());

            if (commit(ref, next, onFailure)) {
                button.setMessage(boolText(next));
            }
        })
            .size(width, 20)
            .build();
    }

    private static Component boolText(Object value) {
        return Boolean.TRUE.equals(value) ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;
    }

    private static Button cycle(OptionRef ref, int width, Runnable onFailure) {
        Button button = Button.builder(ref.display(ref.option.getValue()), pressed -> {
            List<Object> values = ref.cycleValues();

            if (values.size() < 2) {
                return;
            }

            int next = (values.indexOf(ref.option.getValue()) + 1) % values.size();
            Object value = values.get(next);

            if (commit(ref, value, onFailure)) {
                pressed.setMessage(ref.display(value));
            }
        })
            .size(width, 20)
            .build();

        button.active = ref.cycleValues().size() > 1;

        return button;
    }

    private static List<AbstractWidget> text(OptionRef ref, int width, Runnable onFailure) {
        Minecraft minecraft = Minecraft.getInstance();
        EditBox box = new EditBox(minecraft.font, 0, 0, width - SET_WIDTH - 2, 20, ref.name);
        box.setMaxLength(256);
        box.setValue(ref.text(ref.option.getValue()));

        Button set = Button.builder(Component.translatable("mms_settings.opac.set"), pressed -> {
            Object parsed = parse(ref, box.getValue());

            if (parsed != null) {
                commit(ref, parsed, onFailure);
            }
        })
            .size(SET_WIDTH, 20)
            .build();

        box.setResponder(value -> {
            boolean valid = parse(ref, value) != null;
            box.setTextColor(valid ? NORMAL_COLOR : ERROR_COLOR);
            set.active = valid && ref.option.isMutable();
        });

        box.setEditable(ref.option.isMutable());
        set.active = ref.option.isMutable();

        if (!ref.comment.getString().isEmpty()) {
            box.setTooltip(Tooltip.create(ref.comment));
        }

        return List.of(box, set);
    }

    private static Object parse(OptionRef ref, String text) {
        try {
            if (!ref.option.getStringValidator().test(ref.storage, text)) {
                return null;
            }

            Object value = ref.option.getCommandInputParser().apply(text);

            return value != null && ref.option.getValidator().test(ref.storage, value) ? value : null;
        } catch (Throwable error) {
            return null;
        }
    }

    private static boolean commit(OptionRef ref, Object value, Runnable onFailure) {
        boolean valid;

        try {
            valid = ref.option.getValidator().test(ref.storage, value);
        } catch (Throwable error) {
            valid = false;
        }

        if (!valid) {
            return false;
        }

        if (OpacWriter.write(ref.storage, ref.option, value)) {
            return true;
        }

        onFailure.run();

        return false;
    }
}
