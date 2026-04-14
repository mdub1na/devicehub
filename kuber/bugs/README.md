# Known Bugs (Backlog)

## Bug: `ios-provider` crashes when `serial` filter is used

### Status
- Open
- Priority: medium
- Scope: upstream DeviceHub code (not `kuber` manifests/scripts)

### Where it happens
- File: `lib/cli/ios-provider/index.js`
- Current code:
  - `filter: !argv.serial?.length ? null : (serial => argv.serial.find(serial))`

### Symptoms
- `ios-provider` starts, then exits with:
  - `TypeError: argv.serial.find is not a function`
  - or
  - `TypeError: string "..." is not a function`
- Triggered when passing one or more iOS UDIDs to `ios-provider [serial..]`.

### Root cause
- `Array.prototype.find` is called with a string value instead of a predicate function.
- The filter function is invalid and throws during device matching.

### Proposed fix (for later)
- Replace buggy expression with a valid membership check:
  - `filter: !argv.serial?.length ? null : (serial => argv.serial.includes(serial))`

### Temporary workaround (current project policy)
- Do not patch core repository code now.
- Run `ios-provider` without serial filtering and handle device selection operationally.
- Keep this issue documented and return to it later as a dedicated upstream fix.
