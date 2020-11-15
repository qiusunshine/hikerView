onmessage = (event) => {
  const data = event.data.split("%%%%555%%%%");
  eval(data[1]);
  const result = self.hljs.highlightAuto(data[0]);
  postMessage(result.value);
};