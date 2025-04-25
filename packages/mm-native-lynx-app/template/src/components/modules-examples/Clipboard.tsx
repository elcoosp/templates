import { Button } from "@components/Button";

const styles = {
  container: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
  },
  copiedText: {
    marginTop: 10,
    color: "red",
  },
};
export function ClipboardExample() {
  const [copiedText, setCopiedText] = useState("default");

  const copyToClipboard = () => {
    "background only";

    try {
      NativeModules.ClipboardModule.setStringAsync(
        "hello world",
        {
          inputFormat: "plainText",
        },
        () => {},
      );
    } catch (e) {
      setCopiedText(e.toString());
    }
  };

  const fetchCopiedText = () => {
    "background only";

    try {
      const text = NativeModules.ClipboardModule.getStringAsync(
        {
          preferredFormat: "plainText",
        },
        (text) => {
          setCopiedText(text);
        },
      );
    } catch (e) {
      setCopiedText(e.toString());
    }
  };

  return (
    <view style={styles.container}>
      <Button
        title="Click here to copy to Clipboard"
        onPress={copyToClipboard}
      />
      <Button title="View copied text" onPress={fetchCopiedText} />
      <text style={styles.copiedText}>{copiedText}</text>
    </view>
  );
}
