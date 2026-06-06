
export const is_enter = (e, {ctrl=false, shift=false, alt=false}) => {
  if (e.keyCode == 13)  {
    if(ctrl && !e.ctrlKey || !ctrl && e.ctrlKey 
      || shift && !e.shipftKey || !shift && e.shiftKey
      || alt && !e.altKey || !alt && e.altKey) return false
    return true
  }
  return false
}

export const is_space = e => {
  return e.keyCode == 32
}

export const is_left = e => {
  return e.keyCode == 37
}

export const is_right = e => {
  return e.keyCode == 39
}
