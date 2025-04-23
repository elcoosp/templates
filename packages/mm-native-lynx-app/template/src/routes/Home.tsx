import { ClipboardExample } from "@components/modules-examples/Clipboard";

export function Home() {
  const nav = useNavigate();
  return (
    <view>
      <text bindtap={() => nav("/")}>Navigate to App, you are in home</text>
      <ClipboardExample />
    </view>
  );
}
