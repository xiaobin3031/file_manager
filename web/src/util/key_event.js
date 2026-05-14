
export const is_enter = (e, {ctrl=false, shift=false, alt=false}) => {
  if (e.keyCode == 13)  {
    if(ctrl && !e.ctrlKey || !ctrl && e.ctrlKey 
      || shift && !e.shipftKey || !shift && e.shiftKey
      || alt && !e.altKey || !alt && e.altKey) return false
    return true
  }
  return false
}
