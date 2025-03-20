export function Home() {
  const nav = useNavigate()
  return (
    <view>
      <text bindtap={() => nav('/')}>Navigate to App, you are in home</text>
    </view>
  )
}
