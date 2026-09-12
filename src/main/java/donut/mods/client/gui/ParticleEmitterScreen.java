package donut.mods.client.gui;

import donut.mods.client.render.ClientTextureDirectoryCache;
import donut.mods.network.EmitterSettingsPayload;
import donut.mods.network.TextureUploadPayload;
import donut.mods.particle.ParticleEmitterBlockEntity;
import donut.mods.particle.ParticleOption;
import donut.mods.screen.ParticleEmitterScreenHandler;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ParticleEmitterScreen extends HandledScreen<ParticleEmitterScreenHandler> {

    private enum Tab { GENERAL, MOTION, TEXTURE }

    private record Label(int x, int y, String text, boolean header) {}

    private static final int FIELD_HEIGHT = 16;
    private static final int LABEL_COLOR = 0xE0E0E0;
    private static final int HEADER_COLOR = 0xFFD966;
    private static final int WARN_COLOR = 0xFF6B6B;

    private final Map<Tab, List<ClickableWidget>> tabWidgets = new EnumMap<>(Tab.class);
    private final Map<Tab, List<Label>> tabLabels = new EnumMap<>(Tab.class);
    private final Map<Tab, ButtonWidget> tabButtons = new EnumMap<>(Tab.class);
    private Tab currentTab = Tab.GENERAL;

    private CyclingButtonWidget<ParticleOption> particleButton;
    private RangeSlider frequencySlider;
    private RangeSlider countSlider;
    private TextFieldWidget velXField, velYField, velZField;
    private RangeSlider spreadXSlider, spreadYSlider, spreadZSlider;
    private TextureListWidget textureList;
    private TextFieldWidget manualPathField;

    public ParticleEmitterScreen(ParticleEmitterScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 190;
        this.backgroundHeight = 244;
    }

    @Override
    protected void init() {
        super.init();
        this.titleY = 6;
        for (Tab tab : Tab.values()) {
            tabWidgets.put(tab, new ArrayList<>());
            tabLabels.put(tab, new ArrayList<>());
        }

        ParticleEmitterBlockEntity be = getScreenHandler().getBlockEntity();

        ParticleOption initialParticle = be != null ? be.getParticle() : ParticleOption.FLAME;
        int initialFrequency = be != null ? be.getFrequency() : 20;
        float initialVelX = be != null ? be.getVelX() : 0f;
        float initialVelY = be != null ? be.getVelY() : 0.05f;
        float initialVelZ = be != null ? be.getVelZ() : 0f;
        float initialSpreadX = be != null ? be.getSpreadX() : 0.2f;
        float initialSpreadY = be != null ? be.getSpreadY() : 0.2f;
        float initialSpreadZ = be != null ? be.getSpreadZ() : 0.2f;
        int initialCount = be != null ? be.getCount() : 1;
        String initialTextureId = be != null ? be.getCustomTextureId() : "";

        int left = this.x + 8;
        int right = this.x + this.backgroundWidth - 8;
        int fullWidth = right - left;

        int tabY = this.y + 20;
        int tabWidth = fullWidth / 3;
        tabButtons.put(Tab.GENERAL, ButtonWidget.builder(Text.literal("General"), b -> switchTab(Tab.GENERAL))
                .dimensions(left, tabY, tabWidth, 16).build());
        tabButtons.put(Tab.MOTION, ButtonWidget.builder(Text.literal("Motion"), b -> switchTab(Tab.MOTION))
                .dimensions(left + tabWidth, tabY, tabWidth, 16).build());
        tabButtons.put(Tab.TEXTURE, ButtonWidget.builder(Text.literal("Texture"), b -> switchTab(Tab.TEXTURE))
                .dimensions(left + tabWidth * 2, tabY, fullWidth - tabWidth * 2, 16).build());
        tabButtons.values().forEach(this::addDrawableChild);

        int contentTop = tabY + 20;
        int saveButtonY = this.y + this.backgroundHeight - 26;

        // ===================== GENERAL TAB =====================
        int gy = contentTop;
        particleButton = CyclingButtonWidget.<ParticleOption>builder(p -> Text.literal(p.name()))
                .values(ParticleOption.values())
                .initially(initialParticle)
                .build(left, gy, fullWidth, FIELD_HEIGHT, Text.literal("Particle"));
        registerTabWidget(Tab.GENERAL, particleButton);
        gy += FIELD_HEIGHT + 4;

        frequencySlider = new RangeSlider(left, gy, fullWidth, FIELD_HEIGHT, "Frequency", 1, 100, initialFrequency, true, "t");
        registerTabWidget(Tab.GENERAL, frequencySlider);
        gy += FIELD_HEIGHT + 4;

        countSlider = new RangeSlider(left, gy, fullWidth, FIELD_HEIGHT, "Count", 1, 64, initialCount, true, "");
        registerTabWidget(Tab.GENERAL, countSlider);

        // ===================== MOTION TAB =====================
        int my = contentTop;
        tabLabels.get(Tab.MOTION).add(new Label(left, my, "Velocity (per tick)", true));
        my += 10;
        int colWidth = (fullWidth - 8) / 3;
        velXField = smallField(left, my, colWidth, initialVelX);
        velYField = smallField(left + colWidth + 4, my, colWidth, initialVelY);
        velZField = smallField(left + (colWidth + 4) * 2, my, colWidth, initialVelZ);
        registerTabWidget(Tab.MOTION, velXField);
        registerTabWidget(Tab.MOTION, velYField);
        registerTabWidget(Tab.MOTION, velZField);
        my += FIELD_HEIGHT + 6;

        tabLabels.get(Tab.MOTION).add(new Label(left, my, "Spread (blocks, 1-50)", true));
        my += 10;
        spreadXSlider = new RangeSlider(left, my, fullWidth, FIELD_HEIGHT, "X", 1, 50, Math.max(1, Math.round(initialSpreadX)), true, "");
        registerTabWidget(Tab.MOTION, spreadXSlider);
        my += FIELD_HEIGHT + 4;
        spreadYSlider = new RangeSlider(left, my, fullWidth, FIELD_HEIGHT, "Y", 1, 50, Math.max(1, Math.round(initialSpreadY)), true, "");
        registerTabWidget(Tab.MOTION, spreadYSlider);
        my += FIELD_HEIGHT + 4;
        spreadZSlider = new RangeSlider(left, my, fullWidth, FIELD_HEIGHT, "Z", 1, 50, Math.max(1, Math.round(initialSpreadZ)), true, "");
        registerTabWidget(Tab.MOTION, spreadZSlider);

        // ===================== TEXTURE TAB =====================
        int ty = contentTop;
        tabLabels.get(Tab.TEXTURE).add(new Label(left, ty, "Custom Particle Texture", true));
        ty += 12;

        List<String> availableIds = ClientTextureDirectoryCache.getAvailableIds();
        List<String> choices = new ArrayList<>();
        choices.add("");
        choices.addAll(availableIds);

        int rowHeight = 16;
        int gap = 4;
        // Reserve room (bottom-up) for: path row, browse button, headless-status
        // label, gap before Save.
        int reservedBottom = rowHeight + gap + rowHeight + gap + 10 + gap;
        int listHeight = (saveButtonY - gap) - reservedBottom - ty;

        textureList = new TextureListWidget(left, ty, fullWidth, listHeight, choices,
                choices.contains(initialTextureId) ? initialTextureId : "");
        registerTabWidget(Tab.TEXTURE, textureList);
        ty += listHeight + gap;

        // FIX: setMaxLength was missing before, which silently capped input at the
        // default 32 characters - most real file paths are longer than that, so
        // typed/pasted paths were being truncated into nonexistent ones.
        manualPathField = new TextFieldWidget(this.textRenderer, left, ty, fullWidth - 55, rowHeight,
                Text.literal("Local PNG file path"));
        manualPathField.setMaxLength(512);
        registerTabWidget(Tab.TEXTURE, manualPathField);

        ButtonWidget uploadFromPathButton = ButtonWidget.builder(Text.literal("Upload"), btn -> uploadFromManualPath())
                .dimensions(left + fullWidth - 50, ty, 50, rowHeight)
                .build();
        registerTabWidget(Tab.TEXTURE, uploadFromPathButton);
        ty += rowHeight + gap;

        ButtonWidget browseButton = ButtonWidget.builder(Text.literal("Browse..."), btn -> openUploadDialog())
                .dimensions(left, ty, fullWidth, rowHeight)
                .build();
        registerTabWidget(Tab.TEXTURE, browseButton);
        ty += rowHeight + gap;

        // Visible diagnostic - tells you directly whether the native file picker can
        // even work on this setup, without needing to click Browse and hope a chat
        // message appears.
        boolean headless = GraphicsEnvironment.isHeadless();
        tabLabels.get(Tab.TEXTURE).add(new Label(left, ty,
                headless ? "Browse disabled: AWT is headless. Use the path field above." : "Browse available.",
                false));

        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), btn -> sendUpdate())
                .dimensions(left, saveButtonY, fullWidth, 18)
                .build());

        switchTab(Tab.GENERAL);
    }

    private void registerTabWidget(Tab tab, ClickableWidget widget) {
        addDrawableChild(widget);
        tabWidgets.get(tab).add(widget);
    }

    private void switchTab(Tab tab) {
        this.currentTab = tab;
        for (Map.Entry<Tab, List<ClickableWidget>> entry : tabWidgets.entrySet()) {
            boolean active = entry.getKey() == tab;
            for (ClickableWidget widget : entry.getValue()) {
                widget.visible = active;
                widget.active = active;
            }
        }
        for (Map.Entry<Tab, ButtonWidget> entry : tabButtons.entrySet()) {
            entry.getValue().active = entry.getKey() != tab;
        }
    }

    private TextFieldWidget smallField(int x, int y, int width, float initialValue) {
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, y, width, FIELD_HEIGHT, Text.empty());
        field.setText(String.valueOf(initialValue));
        field.setMaxLength(12);
        return field;
    }

    private void uploadFromManualPath() {
        // Strip accidental surrounding quotes - Windows' "Copy as path" wraps the
        // result in double quotes, which would otherwise make the path unresolvable.
        String pathText = manualPathField.getText().trim().replaceAll("^\"|\"$", "");
        if (pathText.isEmpty()) {
            notifyPlayer("Enter a full file path first, e.g. C:/images/star.png");
            return;
        }
        try {
            Path path = Path.of(pathText);
            if (!Files.isRegularFile(path)) {
                notifyPlayer("File not found at: " + path.toAbsolutePath());
                return;
            }
            byte[] data = Files.readAllBytes(path);
            if (data.length > TextureUploadPayload.MAX_BYTES) {
                notifyPlayer("Image too large - max 512 KB.");
                return;
            }
            String fileName = path.getFileName().toString();
            String desiredId = fileName.replaceAll("(?i)\\.png$", "");
            ClientPlayNetworking.send(new TextureUploadPayload(desiredId, data));
            notifyPlayer("Uploading " + desiredId + "...");
        } catch (Exception e) {
            notifyPlayer("Failed: " + e.getMessage());
        }
    }

    private void openUploadDialog() {
        if (GraphicsEnvironment.isHeadless()) {
            notifyPlayer("AWT is headless on this setup - Browse cannot work. Use the path field instead.");
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        boolean wasFullscreen = client.getWindow().isFullscreen();
        if (wasFullscreen) {
            client.getWindow().toggleFullscreen();
        }

        Thread thread = new Thread(() -> {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    javax.swing.JFrame owner = new javax.swing.JFrame();
                    owner.setAlwaysOnTop(true);
                    owner.setUndecorated(true);
                    owner.setSize(1, 1);
                    owner.setLocationRelativeTo(null);
                    owner.setVisible(true);
                    owner.toFront();

                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle("Upload Particle Texture (PNG only)");
                    chooser.setFileFilter(new FileNameExtensionFilter("PNG images", "png"));

                    int result = chooser.showOpenDialog(owner);
                    owner.dispose();

                    if (result != JFileChooser.APPROVE_OPTION) return;

                    File file = chooser.getSelectedFile();
                    try {
                        byte[] data = Files.readAllBytes(file.toPath());
                        if (data.length > TextureUploadPayload.MAX_BYTES) {
                            notifyPlayer("Image too large - max 512 KB.");
                            return;
                        }
                        String desiredId = file.getName().replaceAll("(?i)\\.png$", "");
                        MinecraftClient.getInstance().execute(() ->
                                ClientPlayNetworking.send(new TextureUploadPayload(desiredId, data)));
                        notifyPlayer("Uploading " + desiredId + "...");
                    } catch (IOException e) {
                        notifyPlayer("Failed to read file: " + e.getMessage());
                    }
                });
            } catch (Throwable t) {
                t.printStackTrace();
                notifyPlayer("File picker failed (" + t.getClass().getSimpleName() + ") - use the path field instead.");
            } finally {
                if (wasFullscreen) {
                    client.execute(() -> {
                        if (!client.getWindow().isFullscreen()) {
                            client.getWindow().toggleFullscreen();
                        }
                    });
                }
            }
        }, "particle-emitter-upload-chooser");
        thread.setDaemon(true);
        thread.start();
    }

    private void notifyPlayer(String message) {
        MinecraftClient.getInstance().execute(() -> {
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(Text.literal(message), true);
            }
        });
    }

    private void sendUpdate() {
        ParticleEmitterBlockEntity be = getScreenHandler().getBlockEntity();
        if (be == null) return;

        String textureId = textureList.getSelected();
        ParticleOption selectedParticle = !textureId.isEmpty()
                ? ParticleOption.CUSTOM_IMAGE
                : particleButton.getValue();

        EmitterSettingsPayload payload = new EmitterSettingsPayload(
                getScreenHandler().getPos(),
                selectedParticle.name(),
                (int) frequencySlider.getActualValue(),
                parseFloatSafe(velXField.getText(), 0f),
                parseFloatSafe(velYField.getText(), 0f),
                parseFloatSafe(velZField.getText(), 0f),
                (float) spreadXSlider.getActualValue(),
                (float) spreadYSlider.getActualValue(),
                (float) spreadZSlider.getActualValue(),
                (int) countSlider.getActualValue(),
                textureId
        );

        ClientPlayNetworking.send(payload);
        this.close();
    }

    private static float parseFloatSafe(String s, float fallback) {
        try { return Float.parseFloat(s.trim()); } catch (NumberFormatException e) { return fallback; }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.fill(this.x, this.y, this.x + this.backgroundWidth, this.y + this.backgroundHeight, 0xE0101010);
        context.drawBorder(this.x, this.y, this.backgroundWidth, this.backgroundHeight, 0xFF444444);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        for (Label label : tabLabels.get(currentTab)) {
            int color = label.header() ? HEADER_COLOR
                    : (label.text().startsWith("Browse disabled") ? WARN_COLOR : LABEL_COLOR);
            context.drawText(this.textRenderer, Text.literal(label.text()),
                    label.x(), label.y(), color, false);
        }

        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    private static class RangeSlider extends SliderWidget {
        private final double min;
        private final double max;
        private final String label;
        private final boolean isInt;
        private final String unit;

        RangeSlider(int x, int y, int width, int height, String label,
                    double min, double max, double initialValue, boolean isInt, String unit) {
            super(x, y, width, height, Text.empty(),
                    MathHelper.clamp((initialValue - min) / (max - min), 0.0, 1.0));
            this.label = label;
            this.min = min;
            this.max = max;
            this.isInt = isInt;
            this.unit = unit;
            updateMessage();
        }

        double getActualValue() {
            double raw = min + this.value * (max - min);
            return isInt ? Math.round(raw) : raw;
        }

        @Override
        protected void updateMessage() {
            double val = getActualValue();
            String display = isInt ? String.valueOf((int) val) : String.format("%.2f", val);
            this.setMessage(Text.literal(label + ": " + display + unit));
        }

        @Override
        protected void applyValue() {
        }
    }

    private static class TextureListWidget extends ClickableWidget {
        private static final int ROW_HEIGHT = 14;

        private final List<String> ids;
        private String selectedId;
        private int scrollOffset = 0;

        TextureListWidget(int x, int y, int width, int height, List<String> ids, String initialSelected) {
            super(x, y, width, height, Text.literal("Texture list"));
            this.ids = ids;
            this.selectedId = initialSelected;
        }

        String getSelected() {
            return selectedId;
        }

        private int visibleRows() {
            return Math.max(1, this.getHeight() / ROW_HEIGHT);
        }

        private int maxScroll() {
            return Math.max(0, ids.size() - visibleRows());
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!this.active || !this.visible || button != 0 || !this.isMouseOver(mouseX, mouseY)) {
                return false;
            }
            int rowIndex = (int) ((mouseY - this.getY()) / ROW_HEIGHT) + scrollOffset;
            if (rowIndex >= 0 && rowIndex < ids.size()) {
                selectedId = ids.get(rowIndex);
            }
            return true;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
            if (!this.isMouseOver(mouseX, mouseY)) return false;
            scrollOffset = MathHelper.clamp(scrollOffset - (int) Math.signum(verticalAmount), 0, maxScroll());
            return true;
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x80000000);

            int rows = visibleRows();
            for (int i = 0; i < rows; i++) {
                int idx = i + scrollOffset;
                if (idx >= ids.size()) break;

                String id = ids.get(idx);
                int rowY = getY() + i * ROW_HEIGHT;
                boolean hovered = mouseX >= getX() && mouseX < getX() + getWidth()
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                boolean selected = id.equals(selectedId);

                if (selected) {
                    context.fill(getX(), rowY, getX() + getWidth(), rowY + ROW_HEIGHT, 0xFF3A6EA5);
                } else if (hovered) {
                    context.fill(getX(), rowY, getX() + getWidth(), rowY + ROW_HEIGHT, 0xFF333333);
                }

                String display = id.isEmpty() ? "(none)" : id;
                context.drawText(MinecraftClient.getInstance().textRenderer, display,
                        getX() + 4, rowY + 3, 0xFFFFFFFF, false);
            }

            if (maxScroll() > 0) {
                String hint = (scrollOffset + 1) + "-" + Math.min(ids.size(), scrollOffset + rows) + " / " + ids.size();
                context.drawText(MinecraftClient.getInstance().textRenderer, hint,
                        getX() + getWidth() - MinecraftClient.getInstance().textRenderer.getWidth(hint) - 4,
                        getY() + getHeight() - 10, 0xFFAAAAAA, false);
            }
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            builder.put(NarrationPart.TITLE, Text.literal("Particle texture selection list"));
        }
    }
}