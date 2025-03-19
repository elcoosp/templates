import './App.css'
import arrow from '@assets/arrow.png?inline'
import lynxLogo from '@assets/lynx-logo.png?inline'
import reactLynxLogo from '@assets/react-logo.png?inline'
import { ChangeLang } from '@components/ChangeLang'
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

  const requestFocus = () => {
    lynx
      .createSelectorQuery()
      .select('#input-id')
      .invoke({
        method: 'focus',
        params: {},
        success: function (res) {
          console.log('lynx', 'request focus success')
        },
        fail: function (res) {
          setFocusInputError(JSON.stringify(res))
          console.log('lynx', 'request focus fail')
        },
      })
      .exec()
  }

  return (
    <view className="bg-white shadow-md rounded px-8 pt-6 pb-8 mb-4 flex-1">
      <text className="block text-gray-700 text-sm font-bold mb-2">
        Card URL {focusInputError}
      </text>
      <input
        id="input-id"
        className="shadow appearance-none border rounded w-full py-4 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
        bindinput={handleInput}
        bindtap={requestFocus}
        value={inputValue}
        placeholder="Enter Card URL"
      />
    </view>
  )
}
