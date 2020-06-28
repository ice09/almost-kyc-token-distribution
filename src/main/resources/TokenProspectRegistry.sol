// SPDX-License-Identifier: MIT

pragma solidity ^0.4.25;

import "./ECDSA.sol";

contract TokenProspectRegistry {
    using ECDSA for bytes32;

    mapping(address => bytes32) registry;

    event Registered(address indexed prospect);

    function registerProspect(bytes memory signature) public {
        if (registry[msg.sender] > 0) {
            revert("Signer already registered.");
        }
        bytes32 hash = keccak256(abi.encodePacked("\x19Ethereum Signed Message:\n71", "I AM ", toString(msg.sender), " AND WANT TO RECEIVE YST"));
        address recovered = hash.recover(signature);
        if (recovered != msg.sender) {
            revert("Addresses of signer and sender do not match.");
        }
        registry[recovered] = hash;
        emit Registered(recovered);
    }

    function toString(address account) private pure returns(string memory) {
        return toString(abi.encodePacked(account));
    }

    function toString(bytes memory data) private pure returns(string memory) {
        bytes memory alphabet = "0123456789abcdef";

        bytes memory str = new bytes(2 + data.length * 2);
        str[0] = '0';
        str[1] = 'x';
        for (uint i = 0; i < data.length; i++) {
            str[2+i*2] = alphabet[uint(uint8(data[i] >> 4))];
            str[3+i*2] = alphabet[uint(uint8(data[i] & 0x0f))];
        }
        return string(str);
    }

}
