package wtf.uhoh.newsoftkeyboard.app.keyboards;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Xml;
import androidx.annotation.NonNull;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import wtf.uhoh.newsoftkeyboard.BuildConfig;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;

final class HardKeyboardTranslationXmlParser {

  private static final String TAG = "NSKHardTranslationParser";

  private static final String XML_TRANSLATION_TAG = "PhysicalTranslation";
  private static final String XML_QWERTY_ATTRIBUTE = "QwertyTranslation";
  private static final String XML_SEQUENCE_TAG = "SequenceMapping";
  private static final String XML_KEYS_ATTRIBUTE = "keySequence";
  private static final String XML_TARGET_ATTRIBUTE = "targetChar";
  private static final String XML_TARGET_CHAR_CODE_ATTRIBUTE = "targetCharCode";
  private static final String XML_MULTITAP_TAG = "MultiTap";
  private static final String XML_MULTITAP_KEY_ATTRIBUTE = "key";
  private static final String XML_MULTITAP_CHARACTERS_ATTRIBUTE = "characters";
  private static final String XML_ALT_ATTRIBUTE = "altModifier";
  private static final String XML_SHIFT_ATTRIBUTE = "shiftModifier";

  @NonNull
  static HardKeyboardSequenceHandler parse(
      @NonNull Context context,
      int qwertyTranslationId,
      @NonNull CharSequence keyboardName,
      @NonNull String keyboardId,
      @NonNull String keyboardPackageName) {
    HardKeyboardSequenceHandler translator = new HardKeyboardSequenceHandler();
    try (final XmlResourceParser parser = context.getResources().getXml(qwertyTranslationId)) {
      try {
        int event;
        boolean inTranslations = false;
        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
          String tag = parser.getName();
          if (event == XmlPullParser.START_TAG) {
            if (XML_TRANSLATION_TAG.equals(tag)) {
              inTranslations = true;
              AttributeSet attrs = Xml.asAttributeSet(parser);
              final String qwerty = attrs.getAttributeValue(null, XML_QWERTY_ATTRIBUTE);
              if (qwerty != null) {
                translator.addQwertyTranslation(qwerty);
              }
            } else if (inTranslations && XML_SEQUENCE_TAG.equals(tag)) {
              AttributeSet attrs = Xml.asAttributeSet(parser);

              final int[] keyCodes =
                  getKeyCodesFromPhysicalSequence(
                      attrs.getAttributeValue(null, XML_KEYS_ATTRIBUTE));
              final boolean isAlt = attrs.getAttributeBooleanValue(null, XML_ALT_ATTRIBUTE, false);
              final boolean isShift =
                  attrs.getAttributeBooleanValue(null, XML_SHIFT_ATTRIBUTE, false);
              final String targetChar = attrs.getAttributeValue(null, XML_TARGET_ATTRIBUTE);
              final String targetCharCode =
                  attrs.getAttributeValue(null, XML_TARGET_CHAR_CODE_ATTRIBUTE);
              final int target;
              if (!TextUtils.isEmpty(targetCharCode)) {
                target = Integer.parseInt(targetCharCode);
              } else if (!TextUtils.isEmpty(targetChar)) {
                target = targetChar.charAt(0);
              } else {
                throw new IllegalArgumentException(
                    "both "
                        + XML_TARGET_CHAR_CODE_ATTRIBUTE
                        + " and "
                        + XML_TARGET_ATTRIBUTE
                        + "for key-codes "
                        + Arrays.toString(keyCodes)
                        + " are empty in "
                        + XML_SEQUENCE_TAG
                        + " for keyboard "
                        + keyboardId);
              }

              // asserting
              if (keyCodes.length == 0) {
                Logger.e(
                    TAG,
                    "Physical translator sequence does not include mandatory"
                        + " fields "
                        + XML_KEYS_ATTRIBUTE
                        + " or "
                        + XML_TARGET_ATTRIBUTE);
              } else {
                if (!isAlt && !isShift) {
                  translator.addSequence(keyCodes, target);
                  // http://code.google.com/p/softkeyboard/issues/detail?id=734
                  translator.addShiftSequence(keyCodes, Character.toUpperCase(target));
                } else if (isAlt) {
                  translator.addAltSequence(keyCodes, target);
                } else {
                  translator.addShiftSequence(keyCodes, target);
                }
              }
            } else if (inTranslations && XML_MULTITAP_TAG.equals(tag)) {
              AttributeSet attrs = Xml.asAttributeSet(parser);

              final int[] keyCodes =
                  getKeyCodesFromPhysicalSequence(
                      attrs.getAttributeValue(null, XML_MULTITAP_KEY_ATTRIBUTE));
              if (keyCodes.length != 1) {
                throw new XmlPullParserException(
                    "attribute "
                        + XML_MULTITAP_KEY_ATTRIBUTE
                        + " should contain exactly one key-code when used"
                        + " in "
                        + XML_MULTITAP_TAG
                        + " tag!",
                    parser,
                    new ParseException(XML_MULTITAP_KEY_ATTRIBUTE, parser.getLineNumber()));
              }

              final boolean isAlt = attrs.getAttributeBooleanValue(null, XML_ALT_ATTRIBUTE, false);
              final boolean isShift =
                  attrs.getAttributeBooleanValue(null, XML_SHIFT_ATTRIBUTE, false);
              final String targetCharacters =
                  attrs.getAttributeValue(null, XML_MULTITAP_CHARACTERS_ATTRIBUTE);
              if (TextUtils.isEmpty(targetCharacters) || targetCharacters.length() < 2) {
                throw new XmlPullParserException(
                    "attribute "
                        + XML_MULTITAP_CHARACTERS_ATTRIBUTE
                        + " should contain more than one character when"
                        + " used in "
                        + XML_MULTITAP_TAG
                        + " tag!",
                    parser,
                    new ParseException(XML_MULTITAP_CHARACTERS_ATTRIBUTE, parser.getLineNumber()));
              }

              for (int characterIndex = 0;
                  characterIndex <= targetCharacters.length();
                  characterIndex++) {
                int[] multiTapCodes = new int[characterIndex + 1];
                Arrays.fill(multiTapCodes, keyCodes[0]);
                if (characterIndex < targetCharacters.length()) {
                  final int target = targetCharacters.charAt(characterIndex);

                  if (!isAlt && !isShift) {
                    translator.addSequence(multiTapCodes, target);
                    translator.addShiftSequence(multiTapCodes, Character.toUpperCase(target));
                  } else if (isAlt) {
                    translator.addAltSequence(keyCodes, target);
                  } else {
                    translator.addShiftSequence(keyCodes, target);
                  }
                } else {
                  // and adding the rewind character
                  if (!isAlt && !isShift) {
                    translator.addSequence(multiTapCodes, KeyEventStateMachine.KEYCODE_FIRST_CHAR);
                    translator.addShiftSequence(
                        multiTapCodes, KeyEventStateMachine.KEYCODE_FIRST_CHAR);
                  } else if (isAlt) {
                    translator.addAltSequence(keyCodes, KeyEventStateMachine.KEYCODE_FIRST_CHAR);
                  } else {
                    translator.addShiftSequence(keyCodes, KeyEventStateMachine.KEYCODE_FIRST_CHAR);
                  }
                }
              }
            }
          } else if (event == XmlPullParser.END_TAG && XML_TRANSLATION_TAG.equals(tag)) {
            break;
          }
        }
      } catch (XmlPullParserException e) {
        Logger.e(
            TAG,
            e,
            "Failed to parse keyboard layout. Keyboard '%s' (id %s, package %s),"
                + " translatorResourceId %d",
            keyboardName,
            keyboardId,
            keyboardPackageName,
            qwertyTranslationId);
        if (BuildConfig.DEBUG) throw new RuntimeException("Failed to parse keyboard.", e);
      } catch (IOException e) {
        Logger.e(TAG, e, "Failed to read keyboard file.");
      }
      return translator;
    }
  }

  @NonNull
  private static int[] getKeyCodesFromPhysicalSequence(@NonNull String keyCodesArray) {
    String[] split = keyCodesArray.split(",", -1);
    int[] keyCodes = new int[split.length];
    for (int i = 0; i < keyCodes.length; i++) {
      try {
        keyCodes[i] = Integer.parseInt(split[i]);
      } catch (final NumberFormatException nfe) {
        final String v = split[i];
        try {
          keyCodes[i] = android.view.KeyEvent.class.getField(v).getInt(null);
        } catch (final Exception ex) {
          throw new RuntimeException(ex);
        }
      }
    }

    return keyCodes;
  }
}
