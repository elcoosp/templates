export function BackHandlerEample() {
  const [txt, setTxt] = useState("default");
  useEffect(() => {
    "background only";
    setTimeout(() => {
      try {
        setTxt(NativeModules.BackHandlerModule.test().toString());
      } catch (e) {
        setTxt(e.toString());
      }
    }, 2000);
  });

  return <text style={{ color: "white" }}>{txt}</text>;
}
