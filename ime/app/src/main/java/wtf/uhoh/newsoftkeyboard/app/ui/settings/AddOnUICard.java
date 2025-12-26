package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.os.Parcel;
import android.os.Parcelable;

public class AddOnUICard implements Parcelable {
  private final String packageName;
  private final String title;
  private final String message;
  private final String targetFragment; // Optional: navigation target

  public AddOnUICard(String packageName, String title, String message, String targetFragment) {
    this.packageName = packageName;
    this.title = title;
    this.message = message;
    this.targetFragment = targetFragment;
  }

  protected AddOnUICard(Parcel in) {
    packageName = in.readString();
    title = in.readString();
    message = in.readString();
    targetFragment = in.readString();
  }

  public static final Creator<AddOnUICard> CREATOR =
      new Creator<AddOnUICard>() {
        @Override
        public AddOnUICard createFromParcel(Parcel in) {
          return new AddOnUICard(in);
        }

        @Override
        public AddOnUICard[] newArray(int size) {
          return new AddOnUICard[size];
        }
      };

  public String getPackageName() {
    return packageName;
  }

  public String getTitle() {
    return title;
  }

  public String getMessage() {
    return message;
  }

  public String getTargetFragment() {
    return targetFragment;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(packageName);
    dest.writeString(title);
    dest.writeString(message);
    dest.writeString(targetFragment);
  }
}
