import asyncio
from dataclasses import dataclass, field
from typing import Dict, Any

@dataclass
class SharedState:
    status: Dict[str, Any] = field(default_factory=dict)
    flags: Dict[str, Any] = field(default_factory=dict)
    lock: asyncio.Lock = field(default_factory=asyncio.Lock)

    async def get_status(self) -> Dict[str, Any]:
        async with self.lock:
            return dict(self.status)

    async def set_status(self, status: Dict[str, Any]):
        async with self.lock:
            self.status = status

    async def get_flags(self) -> Dict[str, Any]:
        async with self.lock:
            return dict(self.flags)

    async def set_flags(self, flags: Dict[str, Any]):
        async with self.lock:
            self.flags = flags
