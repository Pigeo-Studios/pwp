using System.Collections.Generic;
using System.Text.Json.Serialization;

namespace PWPLuncher.Models
{
    public class ManifestEntry
    {
        [JsonPropertyName("path")]
        public string Path { get; set; }

        [JsonPropertyName("size")]
        public long Size { get; set; }

        [JsonPropertyName("sha256")]
        public string Sha256 { get; set; }

        [JsonPropertyName("category")]
        public string Category { get; set; }

        [JsonPropertyName("mod_name")]
        public string ModName { get; set; }

        [JsonPropertyName("mod_description")]
        public string ModDescription { get; set; }

        [JsonPropertyName("mod_optional")]
        public bool ModOptional { get; set; }
    }

    public class ManifestResponse
    {
        [JsonPropertyName("files")]
        public List<ManifestEntry> Files { get; set; }

        [JsonPropertyName("total")]
        public int Total { get; set; }
    }

    public class ManifestWrapper
    {
        [JsonPropertyName("success")]
        public bool Success { get; set; }

        [JsonPropertyName("data")]
        public ManifestResponse Data { get; set; }
    }
}
