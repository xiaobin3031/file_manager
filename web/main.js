import './css/main.css'
import { $, $$ } from '#utils/dom.js'

window.onresize = function () {
  // resize();
};

window.addEventListener('load', e => {
  // resize()
})

function resize() {
  const $root = $('body');
  const height = `${window.innerHeight - 10}px`
  const width = `${window.innerWidth - 30}px`
  $root.style.height = height
  $root.style.width = width
}
