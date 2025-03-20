import './App.css'
import arrow from '@assets/arrow.png?inline'
import lynxLogo from '@assets/lynx-logo.png?inline'
import reactLynxLogo from '@assets/react-logo.png?inline'
import { ChangeLang } from '@components/ChangeLang'
import { focusElement } from '@utils/invokers'
export function App() {
  const [alterLogo, setAlterLogo] = useState(false)
  const [brand, setBrand] = useState('')

  useEffect(() => {
    console.info('Hello, ReactLynx')
  }, [])

  const onTap = useCallback(() => {
    'background only'
    setAlterLogo(!alterLogo)
    setBrand(NativeModules.DeviceModule.brand())
    NativeModules.StatusBarModule.setColor()
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
          <text className="Subtitle">with phone brand from native {brand}</text>
          <Input />
        </view>
        <view className="Content">
          <image src={arrow} className="Arrow" />
          <text className="Description">Tap the logo and have fun!</text>
          <text className="Hint">
            Edit
            <text style={{ fontStyle: 'italic' }}>
              {' src/routes/App.tsx '}
            </text>
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
export function Input() {
  const [inputValue, setInputValue] = useState('')
  const [focusInputError, setFocusInputError] = useState('')

  const handleInput = (e) => {
    const currentValue = e.detail.value.trim()
    setInputValue(currentValue)
  }
  const inputId = 'my-invokable-input'
  const requestFocus = () =>
    focusElement(inputId, () => console.log('lynx', 'request focus success'))

  return (
    <input
      id={inputId}
      bindtap={requestFocus}
      className="border rounded-md p-2 block bg-white h-12 w-64"
      text-color="000000"
      bindinput={handleInput}
      value={inputValue}
      placeholder="Enter Card URL"
    />
  )
}
