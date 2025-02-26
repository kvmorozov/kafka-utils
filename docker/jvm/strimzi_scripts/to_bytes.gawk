# Use gawk because gnu awk can't extract regexp groups; gawk has `match`
BEGIN {
  suffixes[""]=1
  suffixes["K"]=1024
  suffixes["M"]=1024**2
  suffixes["G"]=1024**3
  suffixes["T"]=1024**4
}

match($0, /([0-9.]*)([kKmMgGtT]?)/, a) {
  printf("%d", a[1] * suffixes[toupper(a[2])])
}