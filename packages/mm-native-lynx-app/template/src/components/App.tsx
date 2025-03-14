import './App.css'
import arrow from '@assets/arrow.png?inline'
import lynxLogo from '@assets/lynx-logo.png?inline'
import reactLynxLogo from '@assets/react-logo.png?inline'
import { ChangeLang } from '@components/ChangeLang'
export function App() {
  const [alterLogo, setAlterLogo] = useState(false)

  useEffect(() => {
    console.info('Hello, ReactLynx')
  }, [])

  const onTap = useCallback(() => {
    'background only'
    setAlterLogo(!alterLogo)
  }, [alterLogo])

  const nav = useNavigate()
  return (
    <view>
      <view className="Background" />
      <view className="App">
        <view className="Banner">
          <view className="Logo" bindtap={onTap}>
            {alterLogo ? (
              <image src={reactLynxLogo} className="Logo--react" />
            ) : (
              <image src={lynxLogo} className="Logo--lynx" />
            )}
          </view>
          <text className="Title">React</text>
          <text className="Subtitle">on Lynx</text>
        </view>
        <view className="Content">
          <image src={arrow} className="Arrow" />
          <text className="Description">Tap the logo and have fun!</text>
          <text className="Hint">
            Edit<text style={{ fontStyle: 'italic' }}>{' src/App.tsx '}</text>
            to see updates!
          </text>
          <text className="text-3xl font-bold">Hello world from tailwind!</text>
          <ChangeLang />

          <view>
            <text bindtap={() => nav('/home')}>Navigate to Home</text>
          </view>
        </view>
        <view style={{ flex: 1 }}></view>
      </view>
    </view>
  )
}
