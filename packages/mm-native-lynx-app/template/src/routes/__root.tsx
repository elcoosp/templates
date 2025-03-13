import { useNavigate } from '@tanstack/react-router'
export function Route() {
  const nav = useNavigate({ from: "/" })
  return (
    <view>
      <text bindtap={() => nav({ to: '/showcase' })}>
        Navigate to showcase, you are in home
      </text>
    </view>
  )
}
