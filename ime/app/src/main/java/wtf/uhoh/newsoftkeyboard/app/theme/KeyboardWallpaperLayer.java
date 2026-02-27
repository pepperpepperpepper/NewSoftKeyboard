package wtf.uhoh.newsoftkeyboard.app.theme;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.json.JSONObject;

/** A single layer entry in the wallpaper layer stack (background or key-face overlays). */
public record KeyboardWallpaperLayer(
    int type,
    boolean enabled,
    int opacityPercent,
    int blendMode,
    @Nullable Integer argb,
    @Nullable Integer argb2,
    int direction,
    int scalePercent,
    @Nullable List<GradientStop> gradientStops) {

  public record GradientStop(int positionPercent, int argb) {
    public GradientStop {
      positionPercent = clampPercent(positionPercent);
    }
  }

  public static final int TYPE_THEME_TINT = 0;
  public static final int TYPE_DIM = 1;
  public static final int TYPE_SOLID_COLOR = 2;
  public static final int TYPE_GRADIENT = 3;
  public static final int TYPE_VIGNETTE = 4;
  public static final int TYPE_GRAIN = 5;
  public static final int TYPE_COLOR_WASH = 6;
  public static final int TYPE_HIGHLIGHT = 7;
  public static final int TYPE_DOTS = 8;
  public static final int TYPE_GRID = 9;
  public static final int TYPE_STRIPES = 10;
  public static final int TYPE_BLUR = 11;
  public static final int TYPE_CHECKER = 12;
  public static final int TYPE_DIAGONAL_STRIPES = 13;
  public static final int TYPE_TRIANGLES = 14;
  public static final int TYPE_HEX = 15;

  public static final int DIRECTION_VERTICAL = 0;
  public static final int DIRECTION_HORIZONTAL = 1;
  public static final int DIRECTION_VERTICAL_REVERSE = 2;
  public static final int DIRECTION_HORIZONTAL_REVERSE = 3;

  public static final int DEFAULT_SCALE_PERCENT = 100;

  private static final String JSON_KEY_TYPE = "type";
  private static final String JSON_KEY_ENABLED = "enabled";
  private static final String JSON_KEY_OPACITY_PERCENT = "opacity_percent";
  private static final String JSON_KEY_BLEND_MODE = "blend_mode";
  private static final String JSON_KEY_ARGB = "argb";
  private static final String JSON_KEY_ARGB_2 = "argb2";
  private static final String JSON_KEY_DIRECTION = "direction";
  private static final String JSON_KEY_SCALE_PERCENT = "scale_percent";
  private static final String JSON_KEY_GRADIENT_STOPS = "stops";
  private static final String JSON_KEY_GRADIENT_STOP_POS = "pos";
  private static final String JSON_KEY_GRADIENT_STOP_ARGB = "argb";

  @Nullable
  public static KeyboardWallpaperLayer fromJson(@Nullable JSONObject json) {
    if (json == null) return null;

    final String typeString = json.optString(JSON_KEY_TYPE, "");
    final int parsedType = typeFromJson(typeString);
    if (parsedType < 0) return null;

    final boolean enabled = json.optBoolean(JSON_KEY_ENABLED, true);
    final int opacityPercent = clampPercent(json.optInt(JSON_KEY_OPACITY_PERCENT, 100));
    final int blendMode =
        KeyboardWallpaperOverrideConstants.normalizeBlendMode(json.optInt(JSON_KEY_BLEND_MODE, 0));

    final Integer argb = json.has(JSON_KEY_ARGB) ? json.optInt(JSON_KEY_ARGB, 0) : null;
    final Integer argb2 = json.has(JSON_KEY_ARGB_2) ? json.optInt(JSON_KEY_ARGB_2, 0) : null;

    final int direction = directionFromJson(json.optString(JSON_KEY_DIRECTION, "vertical"));
    final int scalePercent =
        clampScalePercent(json.optInt(JSON_KEY_SCALE_PERCENT, DEFAULT_SCALE_PERCENT));

    final List<GradientStop> gradientStops;
    if (parsedType == TYPE_GRADIENT) {
      final org.json.JSONArray stopsArray = json.optJSONArray(JSON_KEY_GRADIENT_STOPS);
      if (stopsArray != null && stopsArray.length() >= 2) {
        final ArrayList<GradientStop> outStops = new ArrayList<>();
        for (int i = 0; i < stopsArray.length(); i++) {
          final JSONObject stopJson = stopsArray.optJSONObject(i);
          if (stopJson == null) continue;
          if (!stopJson.has(JSON_KEY_GRADIENT_STOP_ARGB)) continue;
          final int stopArgb = stopJson.optInt(JSON_KEY_GRADIENT_STOP_ARGB, 0);
          final int stopPos = clampPercent(stopJson.optInt(JSON_KEY_GRADIENT_STOP_POS, 0));
          outStops.add(new GradientStop(stopPos, stopArgb));
        }
        if (outStops.size() >= 2) {
          outStops.sort(Comparator.comparingInt(GradientStop::positionPercent));
          gradientStops = List.copyOf(outStops);
        } else {
          gradientStops = null;
        }
      } else {
        gradientStops = null;
      }
    } else {
      gradientStops = null;
    }

    return new KeyboardWallpaperLayer(
        parsedType,
        enabled,
        opacityPercent,
        blendMode,
        argb,
        argb2,
        direction,
        scalePercent,
        gradientStops);
  }

  @NonNull
  public JSONObject toJson() {
    try {
      final JSONObject json = new JSONObject();
      json.put(JSON_KEY_TYPE, typeToJson(type));
      json.put(JSON_KEY_ENABLED, enabled);
      json.put(JSON_KEY_OPACITY_PERCENT, clampPercent(opacityPercent));
      json.put(
          JSON_KEY_BLEND_MODE, KeyboardWallpaperOverrideConstants.normalizeBlendMode(blendMode));
      if (argb != null) {
        json.put(JSON_KEY_ARGB, argb);
      }
      if (argb2 != null) {
        json.put(JSON_KEY_ARGB_2, argb2);
      }
      if (direction != DIRECTION_VERTICAL) {
        json.put(JSON_KEY_DIRECTION, directionToJson(direction));
      }
      if (scalePercent != DEFAULT_SCALE_PERCENT) {
        json.put(JSON_KEY_SCALE_PERCENT, clampScalePercent(scalePercent));
      }
      if (type == TYPE_GRADIENT && gradientStops != null && gradientStops.size() >= 2) {
        final org.json.JSONArray outStops = new org.json.JSONArray();
        for (GradientStop stop : gradientStops) {
          if (stop == null) continue;
          final JSONObject stopJson = new JSONObject();
          stopJson.put(JSON_KEY_GRADIENT_STOP_POS, clampPercent(stop.positionPercent()));
          stopJson.put(JSON_KEY_GRADIENT_STOP_ARGB, stop.argb());
          outStops.put(stopJson);
        }
        if (outStops.length() >= 2) {
          json.put(JSON_KEY_GRADIENT_STOPS, outStops);
        }
      }
      return json;
    } catch (org.json.JSONException e) {
      return new JSONObject();
    }
  }

  @NonNull
  private static String typeToJson(int type) {
    return switch (type) {
      case TYPE_THEME_TINT -> "theme_tint";
      case TYPE_DIM -> "dim";
      case TYPE_SOLID_COLOR -> "solid";
      case TYPE_GRADIENT -> "gradient";
      case TYPE_VIGNETTE -> "vignette";
      case TYPE_GRAIN -> "grain";
      case TYPE_COLOR_WASH -> "color_wash";
      case TYPE_HIGHLIGHT -> "highlight";
      case TYPE_DOTS -> "dots";
      case TYPE_GRID -> "grid";
      case TYPE_STRIPES -> "stripes";
      case TYPE_BLUR -> "blur";
      case TYPE_CHECKER -> "checker";
      case TYPE_DIAGONAL_STRIPES -> "diagonal_stripes";
      case TYPE_TRIANGLES -> "triangles";
      case TYPE_HEX -> "hex";
      default -> "unknown";
    };
  }

  private static int typeFromJson(@Nullable String type) {
    if (type == null) return -1;
    return switch (type) {
      case "theme_tint" -> TYPE_THEME_TINT;
      case "dim" -> TYPE_DIM;
      case "solid" -> TYPE_SOLID_COLOR;
      case "gradient" -> TYPE_GRADIENT;
      case "vignette" -> TYPE_VIGNETTE;
      case "grain" -> TYPE_GRAIN;
      case "color_wash" -> TYPE_COLOR_WASH;
      case "highlight" -> TYPE_HIGHLIGHT;
      case "dots" -> TYPE_DOTS;
      case "grid" -> TYPE_GRID;
      case "stripes" -> TYPE_STRIPES;
      case "blur" -> TYPE_BLUR;
      case "checker" -> TYPE_CHECKER;
      case "diagonal_stripes" -> TYPE_DIAGONAL_STRIPES;
      case "triangles" -> TYPE_TRIANGLES;
      case "hex" -> TYPE_HEX;
      default -> -1;
    };
  }

  private static int directionFromJson(@Nullable String direction) {
    if (direction == null) return DIRECTION_VERTICAL;
    return switch (direction) {
      case "horizontal" -> DIRECTION_HORIZONTAL;
      case "horizontal_reverse" -> DIRECTION_HORIZONTAL_REVERSE;
      case "vertical", "" -> DIRECTION_VERTICAL;
      case "vertical_reverse" -> DIRECTION_VERTICAL_REVERSE;
      default -> DIRECTION_VERTICAL;
    };
  }

  @NonNull
  private static String directionToJson(int direction) {
    return switch (direction) {
      case DIRECTION_HORIZONTAL -> "horizontal";
      case DIRECTION_HORIZONTAL_REVERSE -> "horizontal_reverse";
      case DIRECTION_VERTICAL -> "vertical";
      case DIRECTION_VERTICAL_REVERSE -> "vertical_reverse";
      default -> "vertical";
    };
  }

  private static int clampPercent(int value) {
    if (value < 0) return 0;
    if (value > 100) return 100;
    return value;
  }

  private static int clampScalePercent(int value) {
    if (value < 25) return 25;
    if (value > 400) return 400;
    return value;
  }
}
