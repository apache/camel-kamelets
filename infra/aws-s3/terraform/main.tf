variable "bucket_name" {
  type = string
}

resource "aws_s3_bucket" "example" {
  bucket = var.bucket_name
}
